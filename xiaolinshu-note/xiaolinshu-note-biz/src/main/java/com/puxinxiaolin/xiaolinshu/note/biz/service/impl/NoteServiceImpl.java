package com.puxinxiaolin.xiaolinshu.note.biz.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.RandomUtil;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.base.Preconditions;
import com.puxinxiaolin.framework.biz.context.holder.LoginUserContextHolder;
import com.puxinxiaolin.framework.common.exception.BizException;
import com.puxinxiaolin.framework.common.response.Response;
import com.puxinxiaolin.framework.common.util.JsonUtils;
import com.puxinxiaolin.xiaolinshu.note.biz.constant.RedisKeyConstants;
import com.puxinxiaolin.xiaolinshu.note.biz.domain.dataobject.NoteDO;
import com.puxinxiaolin.xiaolinshu.note.biz.domain.mapper.NoteDOMapper;
import com.puxinxiaolin.xiaolinshu.note.biz.domain.mapper.TopicDOMapper;
import com.puxinxiaolin.xiaolinshu.note.biz.enums.NoteStatusEnum;
import com.puxinxiaolin.xiaolinshu.note.biz.enums.NoteTypeEnum;
import com.puxinxiaolin.xiaolinshu.note.biz.enums.NoteVisibleEnum;
import com.puxinxiaolin.xiaolinshu.note.biz.enums.ResponseCodeEnum;
import com.puxinxiaolin.xiaolinshu.note.biz.model.vo.FindNoteDetailReqVO;
import com.puxinxiaolin.xiaolinshu.note.biz.model.vo.FindNoteDetailRspVO;
import com.puxinxiaolin.xiaolinshu.note.biz.model.vo.PublishNoteReqVO;
import com.puxinxiaolin.xiaolinshu.note.biz.model.vo.UpdateNoteReqVO;
import com.puxinxiaolin.xiaolinshu.note.biz.rpc.DistributedIdGeneratorRpcService;
import com.puxinxiaolin.xiaolinshu.note.biz.rpc.KeyValueRpcService;
import com.puxinxiaolin.xiaolinshu.note.biz.rpc.UserRpcService;
import com.puxinxiaolin.xiaolinshu.note.biz.service.NoteService;
import com.puxinxiaolin.xiaolinshu.user.api.dto.resp.FindUserByIdRspDTO;
import jakarta.annotation.Resource;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class NoteServiceImpl implements NoteService {
    @Resource
    private NoteDOMapper noteDOMapper;
    @Resource
    private TopicDOMapper topicDOMapper;
    @Resource
    private DistributedIdGeneratorRpcService distributedIdGeneratorRpcService;
    @Resource
    private KeyValueRpcService keyValueRpcService;
    @Resource
    private UserRpcService userRpcService;
    @Resource(name = "taskExecutor")
    private ThreadPoolTaskExecutor threadPoolTaskExecutor;
    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    private static final Cache<Long, String> LOCAL_CACHE = Caffeine.newBuilder()
            .initialCapacity(10000)
            .maximumSize(10000)
            .expireAfterWrite(1, TimeUnit.HOURS)
            .build();

    /**
     * 笔记发布
     *
     * @param request
     * @return
     */
    @Override
    public Response<?> publishNote(PublishNoteReqVO request) {
        Integer type = request.getType();
        NoteTypeEnum noteTypeEnum = NoteTypeEnum.valueOf(type);
        if (Objects.isNull(noteTypeEnum)) {
            throw new BizException(ResponseCodeEnum.NOTE_TYPE_ERROR);
        }

        String imgUris = null;
        // 笔记内容是否为空, 默认值为 true, 即空
        Boolean isContentEmpty = true;
        String videoUri = null;
        switch (noteTypeEnum) {
            case VIDEO -> {
                videoUri = request.getVideoUri();

                Preconditions.checkArgument(StringUtils.isNotBlank(videoUri), "笔记视频不能为空");
            }
            case IMAGE_TEXT -> {
                List<String> imgUriList = request.getImgUris();

                Preconditions.checkArgument(CollUtil.isNotEmpty(imgUriList), "笔记图片不能为空");
                Preconditions.checkArgument(imgUriList.size() <= 8, "笔记图片不能多于 8 张");

                imgUris = StringUtils.join(imgUriList, ",");
            }
            default -> {
            }
        }

        String snowflakeId = distributedIdGeneratorRpcService.getSnowflakeId("note");
        String contentUuid = null;
        String content = request.getContent();
        if (StringUtils.isNotBlank(content)) {
            isContentEmpty = false;
            contentUuid = UUID.randomUUID().toString();
            boolean isSavedSuccess = keyValueRpcService.saveNoteContent(contentUuid, content);

            if (!isSavedSuccess) {
                throw new BizException(ResponseCodeEnum.NOTE_PUBLISH_FAIL);
            }
        }

        Long topicId = request.getTopicId();
        String topicName = null;
        if (Objects.nonNull(topicId)) {
            topicName = topicDOMapper.selectNameByPrimaryKey(topicId);
        }

        Long creatorId = LoginUserContextHolder.getUserId();
        NoteDO noteDO = NoteDO.builder()
                .id(Long.valueOf(snowflakeId))
                .isContentEmpty(isContentEmpty)
                .creatorId(creatorId)
                .imgUris(imgUris)
                .videoUri(videoUri)
                .type(type)
                .topicId(topicId)
                .topicName(topicName)
                .visible(NoteVisibleEnum.PUBLIC.getCode())
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .status(NoteStatusEnum.NORMAL.getCode())
                .title(request.getTitle())
                .isTop(Boolean.FALSE)
                .contentUuid(contentUuid)
                .build();

        try {
            noteDOMapper.insert(noteDO);
        } catch (Exception e) {
            log.error("==> 笔记存储失败", e);

            // RPC: 笔记保存失败, 则删除笔记内容
            if (StringUtils.isNotBlank(contentUuid)) {
                keyValueRpcService.deleteNoteContent(contentUuid);
            }
        }

        return Response.success();
    }

    /**
     * 查询笔记详情
     *
     * @param request
     * @return
     * @optimize 这里 rpc 调用是同步调用的（先 user 再 kv）,
     * 所以用 CompletableFuture 进行异步调用, 统一获取结果后再返回
     */
    @SneakyThrows
    @Override
    public Response<FindNoteDetailRspVO> findNoteDetail(FindNoteDetailReqVO request) {
        Long noteId = request.getId();
        Long userId = LoginUserContextHolder.getUserId();

        String cachedResult = LOCAL_CACHE.getIfPresent(noteId);
        if (cachedResult != null) {
            FindNoteDetailRspVO cachedVo = JsonUtils.parseObject(cachedResult, FindNoteDetailRspVO.class);
            log.info("==> 命中了本地缓存: {}", cachedVo);

            // 可见性校验
            checkNoteVisibleFromVO(userId, cachedVo);
            return Response.success(cachedVo);
        }

        String key = RedisKeyConstants.buildNoteDetailKey(noteId);
        String noteDetailJson = (String) redisTemplate.opsForValue().get(key);
        if (StringUtils.isNotBlank(noteDetailJson)) {
            FindNoteDetailRspVO vo = JsonUtils.parseObject(noteDetailJson, FindNoteDetailRspVO.class);
            // 走异步缓存
            threadPoolTaskExecutor.submit(() ->
                    LOCAL_CACHE.put(noteId, Objects.isNull(vo) ? "null" : JsonUtils.toJsonString(vo))
            );

            // 可见性校验
            checkNoteVisibleFromVO(userId, vo);
            return Response.success(vo);
        }

        NoteDO noteDO = noteDOMapper.selectByPrimaryKey(noteId);
        if (Objects.isNull(noteDO)) {
            threadPoolTaskExecutor.submit(() -> {
                // 防止缓存穿透, 缓存空值（这里过期时间不宜过长）
                long expireSeconds = 60 + RandomUtil.randomInt(60);
                redisTemplate.opsForValue()
                        .set(RedisKeyConstants.NOTE_DETAIL_KEY, "", expireSeconds, TimeUnit.SECONDS);
            });

            throw new BizException(ResponseCodeEnum.NOTE_NOT_FOUND);
        }

        Integer visible = noteDO.getVisible();
        checkNoteVisible(visible, userId, noteDO.getCreatorId());

        // 走 rpc（异步）
        Long creatorId = noteDO.getCreatorId();
        CompletableFuture<FindUserByIdRspDTO> userResultFuture = CompletableFuture.supplyAsync(() ->
                userRpcService.findById(creatorId), threadPoolTaskExecutor
        );

        CompletableFuture<String> contentResultFuture = CompletableFuture.completedFuture(null);
        if (Objects.equals(noteDO.getIsContentEmpty(), Boolean.FALSE)) {
            contentResultFuture = CompletableFuture.supplyAsync(() ->
                    keyValueRpcService.findNoteContent(noteDO.getContentUuid()), threadPoolTaskExecutor);
        }
        
        /*
            对于 CompletableFuture, 如果出现变量被赋值的情况,
             需要额外新开一个变量去存储（转为 final）, 并基于这个新变量进行操作
         */
        CompletableFuture<String> finalContentResultFuture = contentResultFuture;
        CompletableFuture<FindNoteDetailRspVO> resultFuture = CompletableFuture
                // 这里不是 lambda, 所以不用传 final 特性的变量
                .allOf(userResultFuture, contentResultFuture)  // 只有传入的 Future 都完成自身才算完成
                .thenApply(s -> {
                    FindUserByIdRspDTO findUserByIdRspDTO = userResultFuture.join();
                    String content = finalContentResultFuture.join();

                    Integer type = noteDO.getType();
                    String imgUrisStr = noteDO.getImgUris();
                    List<String> imgUris = null;
                    if (Objects.equals(type, NoteTypeEnum.IMAGE_TEXT.getCode())
                            && StringUtils.isNotBlank(imgUrisStr)) {
                        imgUris = List.of(imgUrisStr.split(","));
                    }

                    return FindNoteDetailRspVO.builder()
                            .id(noteDO.getId())
                            .type(noteDO.getType())
                            .title(noteDO.getTitle())
                            .content(content)
                            .imgUris(imgUris)
                            .topicId(noteDO.getTopicId())
                            .topicName(noteDO.getTopicName())
                            .creatorId(noteDO.getCreatorId())
                            .creatorName(findUserByIdRspDTO.getNickName())
                            .avatar(findUserByIdRspDTO.getAvatar())
                            .videoUri(noteDO.getVideoUri())
                            .updateTime(noteDO.getUpdateTime())
                            .visible(noteDO.getVisible())
                            .build();
                });
        FindNoteDetailRspVO vo = resultFuture.get();

        // 异步存入 redis
        threadPoolTaskExecutor.submit(() -> {
            String resultJson = JsonUtils.toJsonString(vo);

            // 过期时间（保底1天 + 随机秒数, 将缓存过期时间打散, 防止同一时间大量缓存失效）
            long expireSeconds = 60 * 60 * 24 + RandomUtil.randomInt(60 * 60 * 24);
            redisTemplate.opsForValue()
                    .set(key, resultJson, expireSeconds, TimeUnit.SECONDS);
        });
        return Response.success(vo);
    }

    /**
     * 更新笔记
     *
     * @param request
     * @return
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Response<?> updateNote(UpdateNoteReqVO request) {
        Long noteId = request.getId();
        Integer type = request.getType();

        NoteTypeEnum noteTypeEnum = NoteTypeEnum.valueOf(type);
        if (noteTypeEnum == null) {
            throw new BizException(ResponseCodeEnum.NOTE_TYPE_ERROR);
        }

        String imgUris = null;
        String videoUri = null;
        switch (noteTypeEnum) {
            case IMAGE_TEXT -> {
                List<String> imgUriList = request.getImgUris();

                Preconditions.checkArgument(CollUtil.isNotEmpty(imgUriList), "笔记图片不能为空");

                imgUris = StringUtils.join(imgUriList, ",");
            }
            case VIDEO -> {
                videoUri = request.getVideoUri();

                Preconditions.checkArgument(StringUtils.isNotBlank(videoUri), "笔记视频不能为空");
            }
            default -> {
            }
        }

        Long topicId = request.getTopicId();
        String topicName = null;
        if (topicId != null) {
            topicName = topicDOMapper.selectNameByPrimaryKey(topicId);
            if (StringUtils.isBlank(topicName)) {
                throw new BizException(ResponseCodeEnum.TOPIC_NOT_FOUND);
            }
        }
        
        String content = request.getContent();
        NoteDO noteDO = NoteDO.builder()
                .id(noteId)
                .isContentEmpty(StringUtils.isBlank(content))
                .imgUris(imgUris)
                .title(request.getTitle())
                .topicId(request.getTopicId())
                .topicName(topicName)
                .type(type)
                .updateTime(LocalDateTime.now())
                .videoUri(videoUri)
                .build();
        noteDOMapper.updateByPrimaryKeySelective(noteDO);
        
        // 删除缓存（redis + caffeine）
        String key = RedisKeyConstants.buildNoteDetailKey(noteId);
        redisTemplate.delete(key);
        
        LOCAL_CACHE.invalidate(noteId);

        NoteDO existed = noteDOMapper.selectByPrimaryKey(noteId);
        String contentUuid = existed.getContentUuid();
        
        boolean isUpdatedSuccess = false;
        if (StringUtils.isBlank(content)) { 
            isUpdatedSuccess = keyValueRpcService.deleteNoteContent(contentUuid);
        } else {
            contentUuid = StringUtils.isBlank(contentUuid) ? UUID.randomUUID().toString() : contentUuid;
            
            isUpdatedSuccess = keyValueRpcService.saveNoteContent(contentUuid, content);
        }
        
        if (!isUpdatedSuccess) {
            throw new BizException(ResponseCodeEnum.NOTE_UPDATE_FAIL);
        }
        
        return Response.success();
    }

    /**
     * 检查笔记是否可见（针对 VO）
     *
     * @param userId
     * @param vo
     */
    private void checkNoteVisibleFromVO(Long userId, FindNoteDetailRspVO vo) {
        if (Objects.nonNull(vo)) {
            Integer visible = vo.getVisible();
            checkNoteVisible(visible, userId, vo.getCreatorId());
        }
    }

    /**
     * 检查笔记是否可见
     *
     * @param visible
     * @param userId
     * @param creatorId
     */
    private void checkNoteVisible(Integer visible, Long userId, Long creatorId) {
        if (Objects.equals(visible, NoteVisibleEnum.PRIVATE.getCode())
                && !Objects.equals(userId, creatorId)) {
            throw new BizException(ResponseCodeEnum.NOTE_PRIVATE);
        }
    }

}
