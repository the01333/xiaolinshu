package com.puxinxiaolin.xiaolinshu.note.biz.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.RandomUtil;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.puxinxiaolin.framework.biz.context.holder.LoginUserContextHolder;
import com.puxinxiaolin.framework.common.exception.BizException;
import com.puxinxiaolin.framework.common.response.Response;
import com.puxinxiaolin.framework.common.util.DateUtils;
import com.puxinxiaolin.framework.common.util.JsonUtils;
import com.puxinxiaolin.xiaolinshu.note.biz.constant.MQConstants;
import com.puxinxiaolin.xiaolinshu.note.biz.constant.RedisKeyConstants;
import com.puxinxiaolin.xiaolinshu.note.biz.domain.dataobject.NoteCollectionDO;
import com.puxinxiaolin.xiaolinshu.note.biz.domain.dataobject.NoteDO;
import com.puxinxiaolin.xiaolinshu.note.biz.domain.dataobject.NoteLikeDO;
import com.puxinxiaolin.xiaolinshu.note.biz.domain.mapper.NoteCollectionDOMapper;
import com.puxinxiaolin.xiaolinshu.note.biz.domain.mapper.NoteDOMapper;
import com.puxinxiaolin.xiaolinshu.note.biz.domain.mapper.NoteLikeDOMapper;
import com.puxinxiaolin.xiaolinshu.note.biz.domain.mapper.TopicDOMapper;
import com.puxinxiaolin.xiaolinshu.note.biz.enums.*;
import com.puxinxiaolin.xiaolinshu.note.biz.model.dto.CollectUnCollectNoteMqDTO;
import com.puxinxiaolin.xiaolinshu.note.biz.model.dto.LikeUnLikeNoteMqDTO;
import com.puxinxiaolin.xiaolinshu.note.biz.model.dto.NoteOperateMqDTO;
import com.puxinxiaolin.xiaolinshu.note.biz.model.vo.*;
import com.puxinxiaolin.xiaolinshu.note.biz.rpc.DistributedIdGeneratorRpcService;
import com.puxinxiaolin.xiaolinshu.note.biz.rpc.KeyValueRpcService;
import com.puxinxiaolin.xiaolinshu.note.biz.rpc.UserRpcService;
import com.puxinxiaolin.xiaolinshu.note.biz.service.NoteService;
import com.puxinxiaolin.xiaolinshu.user.api.dto.resp.FindUserByIdRspDTO;
import jakarta.annotation.Resource;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
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
    private NoteLikeDOMapper noteLikeDOMapper;
    @Resource
    private NoteCollectionDOMapper noteCollectionDOMapper;
    @Resource
    private DistributedIdGeneratorRpcService distributedIdGeneratorRpcService;
    @Resource
    private KeyValueRpcService keyValueRpcService;
    @Resource
    private UserRpcService userRpcService;
    @Resource(name = "taskExecutor")
    private ThreadPoolTaskExecutor threadPoolTaskExecutor;
    @Resource
    private RedisTemplate<String, String> redisTemplate;
    @Resource
    private RocketMQTemplate rocketMQTemplate;

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
        boolean isContentEmpty = true;
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

        // 走 RPC
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
        
        // 走 MQ, 同步缓存
        NoteOperateMqDTO mqDTO = NoteOperateMqDTO.builder()
                .creatorId(creatorId)
                .noteId(Long.valueOf(snowflakeId))
                .type(NoteOperateEnum.PUBLISH.getCode())
                .build();

        Message<String> message = MessageBuilder.withPayload(JsonUtils.toJsonString(mqDTO))
                .build();
        String destination = MQConstants.TOPIC_NOTE_OPERATE + ":" + MQConstants.TAG_NOTE_PUBLISH;
        
        rocketMQTemplate.asyncSend(destination, message, new SendCallback() {
            @Override
            public void onSuccess(SendResult sendResult) {
                log.info("==> 【笔记发布】MQ 发送成功, SendResult: {}", sendResult);
            }

            @Override
            public void onException(Throwable throwable) {
                log.error("==> 【笔记发布】MQ 发送异常: ", throwable);
            }
        });

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
        String noteDetailJson = redisTemplate.opsForValue().get(key);
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

        Long currUserId = LoginUserContextHolder.getUserId();
        NoteDO exist = noteDOMapper.selectByPrimaryKey(noteId);
        if (exist == null) {
            throw new BizException(ResponseCodeEnum.NOTE_NOT_FOUND);
        }
        if (!Objects.equals(currUserId, exist.getCreatorId())) {
            throw new BizException(ResponseCodeEnum.NOTE_CANT_OPERATE);
        }

        Long topicId = request.getTopicId();
        String topicName = null;
        if (topicId != null) {
            topicName = topicDOMapper.selectNameByPrimaryKey(topicId);
            if (StringUtils.isBlank(topicName)) {
                throw new BizException(ResponseCodeEnum.TOPIC_NOT_FOUND);
            }
        }

        // 删除缓存（redis + caffeine）
        String key = RedisKeyConstants.buildNoteDetailKey(noteId);
        redisTemplate.delete(key);

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

        // 一致性保证: 延迟双删策略（用延时消息代替 redis 直接删除缓存）
        // 异步发送延时消息
        Message<String> message = MessageBuilder.withPayload(String.valueOf(noteId))
                .build();
        rocketMQTemplate.asyncSend(MQConstants.TOPIC_DELAY_DELETE_NOTE_REDIS_CACHE, message, new SendCallback() {
            @Override
            public void onSuccess(SendResult sendResult) {
                log.info("## 延时删除 Redis 笔记缓存消息发送成功...");
            }

            @Override
            public void onException(Throwable e) {
                log.error("## 延时删除 Redis 笔记缓存消息发送失败...", e);
            }
        }, 3000, 1);

        // 用 broadcast 模式把所有实例的本地缓存都删除掉
        removeLocalCacheByMQBroadcast(noteId);

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
     * 删除本地笔记缓存
     *
     * @param noteId
     */
    @Override
    public void deleteNoteLocalCache(Long noteId) {
        LOCAL_CACHE.invalidate(noteId);
    }

    /**
     * 删除笔记
     *
     * @param request
     * @return
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Response<?> deleteNote(DeleteNoteReqVO request) {
        Long noteId = request.getId();
        NoteDO exist = noteDOMapper.selectByPrimaryKey(noteId);
        if (exist == null) {
            throw new BizException(ResponseCodeEnum.NOTE_NOT_FOUND);
        }

        Long currUserId = LoginUserContextHolder.getUserId();
        if (!Objects.equals(currUserId, exist.getCreatorId())) {
            throw new BizException(ResponseCodeEnum.NOTE_CANT_OPERATE);
        }

        NoteDO noteDO = NoteDO.builder()
                .id(noteId)
                .status(NoteStatusEnum.DELETED.getCode())
                .updateTime(LocalDateTime.now())
                .build();

        int count = noteDOMapper.updateByPrimaryKeySelective(noteDO);
        if (count == 0) {
            throw new BizException(ResponseCodeEnum.NOTE_NOT_FOUND);
        }

        String key = RedisKeyConstants.buildNoteDetailKey(noteId);
        redisTemplate.delete(key);

        // 用 broadcast 模式把所有实例的本地缓存都删除掉
        removeLocalCacheByMQBroadcast(noteId);

        log.info("====> MQ: 删除笔记本地缓存发送成功...");

        // 走 MQ, 同步缓存
        NoteOperateMqDTO mqDTO = NoteOperateMqDTO.builder()
                .creatorId(exist.getCreatorId())
                .noteId(noteId)
                .type(NoteOperateEnum.DELETE.getCode())
                .build();

        Message<String> message = MessageBuilder.withPayload(JsonUtils.toJsonString(mqDTO))
                .build();
        String destination = MQConstants.TOPIC_NOTE_OPERATE + ":" + MQConstants.TAG_NOTE_DELETE;

        rocketMQTemplate.asyncSend(destination, message, new SendCallback() {
            @Override
            public void onSuccess(SendResult sendResult) {
                log.info("==> 【笔记删除】MQ 发送成功, SendResult: {}", sendResult);
            }

            @Override
            public void onException(Throwable throwable) {
                log.error("==> 【笔记删除】MQ 发送异常: ", throwable);
            }
        });
        
        return Response.success();
    }

    /**
     * 笔记仅对自己可见
     *
     * @param request
     * @return
     */
    @Override
    public Response<?> visibleOnlyMe(UpdateNoteVisibleOnlyMeReqVO request) {
        Long noteId = request.getId();
        NoteDO exist = noteDOMapper.selectByPrimaryKey(noteId);
        if (Objects.isNull(exist)) {
            throw new BizException(ResponseCodeEnum.NOTE_NOT_FOUND);
        }

        Long currUserId = LoginUserContextHolder.getUserId();
        if (!Objects.equals(currUserId, exist.getCreatorId())) {
            throw new BizException(ResponseCodeEnum.NOTE_CANT_OPERATE);
        }

        NoteDO noteDO = NoteDO.builder()
                .id(noteId)
                .visible(NoteVisibleEnum.PRIVATE.getCode())
                .updateTime(LocalDateTime.now())
                .build();

        int count = noteDOMapper.updateVisibleOnlyMe(noteDO);
        if (count == 0) {
            throw new BizException(ResponseCodeEnum.NOTE_CANT_VISIBLE_ONLY_ME);
        }

        String noteDetailRedisKey = RedisKeyConstants.buildNoteDetailKey(noteId);
        redisTemplate.delete(noteDetailRedisKey);

        // 用 broadcast 模式把所有实例的本地缓存都删除掉
        removeLocalCacheByMQBroadcast(noteId);

        return Response.success();

    }

    /**
     * 笔记置顶 / 取消置顶
     *
     * @param request
     * @return
     */
    @Override
    public Response<?> topNote(TopNoteReqVO request) {
        Long noteId = request.getId();
        Boolean isTop = request.getIsTop();
        Long currentUserId = LoginUserContextHolder.getUserId();

        NoteDO noteDO = NoteDO.builder()
                .id(noteId)
                .isTop(isTop)
                .updateTime(LocalDateTime.now())
                .creatorId(currentUserId)
                .build();

        int count = noteDOMapper.updateIsTop(noteDO);
        if (count == 0) {
            throw new BizException(ResponseCodeEnum.NOTE_CANT_OPERATE);
        }

        String key = RedisKeyConstants.buildNoteDetailKey(noteId);
        redisTemplate.delete(key);

        // 用 broadcast 模式把所有实例的本地缓存都删除掉
        removeLocalCacheByMQBroadcast(noteId);

        return Response.success();
    }

    /**
     * 点赞笔记
     *
     * @param request
     * @return
     */
    @Override
    public Response<?> likeNote(LikeNoteReqVO request) {
        Long noteId = request.getId();

        // 判断笔记是否存在
        Long creatorId = checkNoteIsExist(noteId);

        // 判断笔记是否已点赞过
        Long userId = LoginUserContextHolder.getUserId();
        String bloomUserNoteLikeListKey = RedisKeyConstants.buildBloomUserNoteLikeListKey(userId);
        String userNoteLikeZSetKey = RedisKeyConstants.buildUserNoteLikeZSetKey(userId);

        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setScriptSource(new ResourceScriptSource(new ClassPathResource("/lua/bloom_note_like_check.lua")));
        script.setResultType(Long.class);

        Long result = redisTemplate.execute(script, Collections.singletonList(bloomUserNoteLikeListKey), noteId);
        NoteLikeLuaResultEnum noteLikeLuaResultEnum = NoteLikeLuaResultEnum.valueOf(result);
        switch (noteLikeLuaResultEnum) {
            case NOT_EXIST -> {
                // 从数据库中校验笔记是否被点赞, 并异步初始化布隆过滤器, 设置过期时间
                long expireSeconds = 60 * 60 * 24 + RandomUtil.randomInt(60 * 60 * 24);

                int count = noteLikeDOMapper.selectCountByUserIdAndNoteId(userId, noteId);
                if (count > 0) {
                    // 异步初始化布隆过滤器
                    threadPoolTaskExecutor.submit(() ->
                            batchAddNoteLike2BloomAndExpire(userId, expireSeconds, bloomUserNoteLikeListKey));

                    throw new BizException(ResponseCodeEnum.NOTE_ALREADY_LIKED);
                }

                // 若目标笔记未被点赞, 查询当前用户是否有点赞其他笔记, 有则同步初始化布隆过滤器
                batchAddNoteLike2BloomAndExpire(userId, expireSeconds, bloomUserNoteLikeListKey);

                script.setScriptSource(new ResourceScriptSource(new ClassPathResource("/lua/bloom_add_note_like_and_expire.lua")));
                script.setResultType(Long.class);

                redisTemplate.execute(script, Collections.singletonList(bloomUserNoteLikeListKey), noteId, expireSeconds);
            }
            case NOTE_LIKED -> {
                // 布隆过滤器可能出现误判, 需要进一步判断（zset + DB）
                Double score = redisTemplate.opsForZSet().score(userNoteLikeZSetKey, noteId);
                if (score != null) {
                    throw new BizException(ResponseCodeEnum.NOTE_ALREADY_LIKED);
                }

                int count = noteLikeDOMapper.selectNoteIsLiked(userId, noteId);
                if (count > 0) {
                    // 如果 zset 不存在, 需要重新异步初始化 zset, 避免一直走 DB 而不走 zset
                    asyncInitUserNoteLikesZSet(userId, userNoteLikeZSetKey);

                    throw new BizException(ResponseCodeEnum.NOTE_ALREADY_LIKED);
                }
            }
        }

        // 更新 zset 点赞列表
        LocalDateTime now = LocalDateTime.now();
        script.setScriptSource(new ResourceScriptSource(new ClassPathResource("/lua/note_like_check_and_update_zset.lua")));
        script.setResultType(Long.class);

        result = redisTemplate.execute(script, Collections.singletonList(userNoteLikeZSetKey), noteId, DateUtils.localDateTime2Timestamp(now));
        // 如果 zset 不存在, 重新初始化 zset
        if (Objects.equals(result, NoteLikeLuaResultEnum.NOT_EXIST.getCode())) {
            long expireSeconds = 60 * 60 * 24 + RandomUtil.randomInt(60 * 60 * 24);

            DefaultRedisScript<Long> script2 = new DefaultRedisScript<>();
            script2.setScriptSource(new ResourceScriptSource(new ClassPathResource("/lua/batch_add_note_like_zset_and_expire.lua")));
            script2.setResultType(Long.class);

            // 查询用户最新点赞的最新 100 篇
            List<NoteLikeDO> noteLikeDOList = noteLikeDOMapper.selectLikedByUserIdAndLimit(userId, 100);
            if (CollUtil.isNotEmpty(noteLikeDOList)) {
                Object[] luaArgs = buildNoteLikeZSetLuaArgs(noteLikeDOList, expireSeconds);

                redisTemplate.execute(script2, Collections.singletonList(userNoteLikeZSetKey), luaArgs);

                redisTemplate.execute(script, Collections.singletonList(userNoteLikeZSetKey), noteId, DateUtils.localDateTime2Timestamp(now));
            } else {
                List<Object> luaArgs = Lists.newArrayList();
                luaArgs.add(DateUtils.localDateTime2Timestamp(now));
                luaArgs.add(noteId);
                luaArgs.add(expireSeconds);

                redisTemplate.execute(script2, Collections.singletonList(userNoteLikeZSetKey), luaArgs.toArray());
            }

        }

        // 走 MQ, 落地点赞数据入库
        LikeUnLikeNoteMqDTO mqDTO = LikeUnLikeNoteMqDTO.builder()
                .userId(userId)
                .noteId(noteId)
                .type(LikeUnlikeNoteTypeEnum.LIKE.getCode())
                .createTime(now)
                .noteCreatorId(creatorId)
                .build();

        Message<String> message = MessageBuilder.withPayload(JsonUtils.toJsonString(mqDTO))
                .build();
        String destination = MQConstants.TOPIC_LIKE_OR_UNLIKE + ":" + MQConstants.TAG_LIKE;
        // 顺序消息需要用 hashKey 去选择队列
        String hashKey = String.valueOf(userId);

        rocketMQTemplate.asyncSendOrderly(destination, message, hashKey, new SendCallback() {
            @Override
            public void onSuccess(SendResult sendResult) {
                log.info("==> 【笔记点赞】MQ 发送成功, SendResult: {}", JsonUtils.toJsonString(sendResult));
            }

            @Override
            public void onException(Throwable throwable) {
                log.error("==> 【笔记点赞】MQ 发送异常: {}", throwable.getMessage(), throwable);
            }
        });

        return Response.success();
    }

    /**
     * 取消点赞笔记
     *
     * @param request
     * @return
     */
    @Override
    public Response<?> unlikeNote(UnlikeNoteReqVO request) {

        Long noteId = request.getId();

        // 1. 判断笔记是否存在
        Long creatorId = checkNoteIsExist(noteId);

        // 2. 判断是否点赞过笔记
        Long userId = LoginUserContextHolder.getUserId();
        String bloomUserNoteLikeListKey = RedisKeyConstants.buildBloomUserNoteLikeListKey(userId);

        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setScriptSource(new ResourceScriptSource(new ClassPathResource("/lua/bloom_note_unlike_check.lua")));
        script.setResultType(Long.class);

        Long result = redisTemplate.execute(script, Collections.singletonList(bloomUserNoteLikeListKey), noteId);
        NoteUnlikeLuaResultEnum resultEnum = NoteUnlikeLuaResultEnum.valueOf(result);
        switch (resultEnum) {
            case NOT_EXIST -> {
                threadPoolTaskExecutor.submit(() -> {
                    long expireSeconds = 60 * 60 * 24 + RandomUtil.randomInt(60 * 60 * 24);

                    batchAddNoteLike2BloomAndExpire(noteId, expireSeconds, bloomUserNoteLikeListKey);
                });

                int count = noteLikeDOMapper.selectCountByUserIdAndNoteId(userId, noteId);
                if (count == 0) {
                    throw new BizException(ResponseCodeEnum.NOTE_NOT_LIKED);
                }
            }
            case NOTE_NOT_LIKED -> throw new BizException(ResponseCodeEnum.NOTE_NOT_LIKED);
        }

        // 3. 删除 zset 中已点赞的笔记 ID
        // 能走到这里, 说明布隆过滤器判断已点赞, 直接删除 ZSET 中已点赞的笔记 ID
        String userNoteLikeZSetKey = RedisKeyConstants.buildUserNoteLikeZSetKey(userId);
        redisTemplate.opsForZSet().remove(userNoteLikeZSetKey, noteId);

        // 4. 走 MQ, 入库
        LikeUnLikeNoteMqDTO mqDTO = LikeUnLikeNoteMqDTO.builder()
                .noteId(noteId)
                .userId(userId)
                .type(LikeUnlikeNoteTypeEnum.UNLIKE.getCode())
                .createTime(LocalDateTime.now())
                .noteCreatorId(creatorId)
                .build();
        Message<String> message = MessageBuilder.withPayload(JsonUtils.toJsonString(mqDTO))
                .build();

        String destination = MQConstants.TOPIC_LIKE_OR_UNLIKE + ":" + MQConstants.TAG_UNLIKE;
        // 顺序消息需要用 hashKey 去选择队列
        String hashKey = String.valueOf(userId);

        rocketMQTemplate.asyncSendOrderly(destination, message, hashKey, new SendCallback() {
            @Override
            public void onSuccess(SendResult sendResult) {
                log.info("==> 【笔记取消点赞】MQ 发送成功, SendResult: {}", sendResult);
            }

            @Override
            public void onException(Throwable throwable) {
                log.error("==> 【笔记取消点赞】MQ 发送异常: ", throwable);
            }
        });

        return Response.success();
    }

    /**
     * 收藏笔记
     *
     * @param request
     * @return
     */
    @Override
    public Response<?> collectNote(CollectNoteReqVO request) {

        Long noteId = request.getNoteId();

        // 1. 判断笔记是否存在
        Long creatorId = checkNoteIsExist(noteId);

        // 2. 判断是否已收藏过
        Long userId = LoginUserContextHolder.getUserId();
        String userNoteCollectListKey = RedisKeyConstants.buildBloomUserNoteCollectListKey(userId);
        String userNoteCollectZSetKey = RedisKeyConstants.buildUserNoteCollectZSetKey(userId);

        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setResultType(Long.class);
        script.setScriptSource(new ResourceScriptSource(new ClassPathResource("/lua/bloom_note_collect_check.lua")));

        Long result = redisTemplate.execute(script, Collections.singletonList(userNoteCollectListKey), noteId);
        NoteCollectLuaResultEnum resultEnum = NoteCollectLuaResultEnum.valueOf(result);

        switch (resultEnum) {
            // Redis 中布隆过滤器不存在
            case NOT_EXIST -> {
                // 从数据库中校验笔记是否被点赞, 并异步初始化布隆过滤器, 设置过期时间
                long expireSeconds = 60 * 60 * 24 + RandomUtil.randomInt(60 * 60 * 24);

                int count = noteCollectionDOMapper.selectCountByUserIdAndNoteId(userId, noteId);
                // 如果目标笔记已收藏, 初始化布隆过滤器
                if (count > 0) {
                    threadPoolTaskExecutor.submit(() ->
                            batchAddNoteCollect2BloomAndExpire(userId, expireSeconds, userNoteCollectListKey));

                    throw new BizException(ResponseCodeEnum.NOTE_ALREADY_COLLECTED);
                }

                // 若目标笔记未被收藏, 查询当前用户是否有收藏其他笔记, 有则同步初始化布隆过滤器
                batchAddNoteCollect2BloomAndExpire(userId, expireSeconds, userNoteCollectListKey);

                // 添加当前收藏笔记 ID 到布隆过滤器中
                script.setResultType(Long.class);
                script.setScriptSource(new ResourceScriptSource(new ClassPathResource("/lua/bloom_add_note_collect_and_expire.lua")));
                redisTemplate.execute(script, Collections.singletonList(userNoteCollectListKey), noteId, expireSeconds);
            }
            // 目标笔记已经被收藏 (可能存在误判, 需要进一步确认)
            case NOTE_COLLECTED -> {
                // 先查 redis zset
                Double score = redisTemplate.opsForZSet().score(userNoteCollectZSetKey, noteId);
                if (score != null) {
                    throw new BizException(ResponseCodeEnum.NOTE_ALREADY_COLLECTED);
                }

                int count = noteCollectionDOMapper.selectCountByUserIdAndNoteId(userId, noteId);
                if (count > 0) {
                    // 若数据库里面有收藏记录, 而 Redis 中 ZSet 已过期被删除的话, 需要重新异步初始化 ZSet
                    asyncInitUserNoteCollectsZSet(userId, userNoteCollectZSetKey);

                    throw new BizException(ResponseCodeEnum.NOTE_ALREADY_COLLECTED);
                }
            }
        }

        // 3. 更新用户 ZSET 收藏列表
        LocalDateTime now = LocalDateTime.now();
        script.setScriptSource(new ResourceScriptSource(new ClassPathResource("/lua/note_collect_check_and_update_zset.lua")));
        script.setResultType(Long.class);

        result = redisTemplate.execute(script, Collections.singletonList(userNoteCollectZSetKey), noteId, DateUtils.localDateTime2Timestamp(now));
        // 如果不存在需要重新初始化 
        if (Objects.equals(result, NoteCollectLuaResultEnum.NOT_EXIST.getCode())) {
            List<NoteCollectionDO> noteCollectionDOS = noteCollectionDOMapper.selectCollectedByUserIdAndLimit(userId, 300);

            long expireSeconds = 60 * 60 * 24 + RandomUtil.randomInt(60 * 60 * 24);

            DefaultRedisScript<Long> script2 = new DefaultRedisScript<>();
            script2.setResultType(Long.class);
            script2.setScriptSource(new ResourceScriptSource(new ClassPathResource("/lua/batch_add_note_collect_zset_and_expire.lua")));

            if (CollUtil.isNotEmpty(noteCollectionDOS)) {
                Object[] luaArgs = buildNoteCollectZSetLuaArgs(noteCollectionDOS, expireSeconds);
                redisTemplate.execute(script2, Collections.singletonList(userNoteCollectZSetKey), luaArgs);

                // 当前收藏的笔记也要加入 zset
                redisTemplate.execute(script, Collections.singletonList(userNoteCollectZSetKey), noteId, DateUtils.localDateTime2Timestamp(now));
            } else {
                // 如果没有历史收藏的笔记, 将当前收藏的笔记加入 zset, 随机过期时间
                List<Object> luaArgs = Lists.newArrayList();
                luaArgs.add(DateUtils.localDateTime2Timestamp(LocalDateTime.now()));
                luaArgs.add(noteId);
                luaArgs.add(expireSeconds);

                redisTemplate.execute(script2, Collections.singletonList(userNoteCollectZSetKey), luaArgs.toArray());
            }
        }

        // 4. 走 MQ, 数据入库
        CollectUnCollectNoteMqDTO mqDTO = CollectUnCollectNoteMqDTO.builder()
                .userId(userId)
                .noteId(noteId)
                .noteCreatorId(creatorId)
                .build();

        Message<String> message = MessageBuilder.withPayload(JsonUtils.toJsonString(mqDTO))
                .build();

        String destination = MQConstants.TOPIC_COLLECT_OR_UN_COLLECT + ":" + MQConstants.TAG_COLLECT;
        String hashKey = String.valueOf(userId);

        rocketMQTemplate.asyncSendOrderly(destination, message, hashKey, new SendCallback() {
            @Override
            public void onSuccess(SendResult sendResult) {
                log.info("==> 【笔记收藏】MQ 发送成功, SendResult: {}", sendResult);
            }

            @Override
            public void onException(Throwable throwable) {
                log.error("==> 【笔记收藏】MQ 发送异常: ", throwable);
            }
        });

        return Response.success();
    }

    /**
     * 取消收藏笔记
     *
     * @param request
     * @return
     */
    @Override
    public Response<?> unCollectNote(UnCollectNoteReqVO request) {
        Long noteId = request.getId();

        // 1. 校验笔记是否存在
        Long creatorId = checkNoteIsExist(noteId);

        // 2. 校验笔记是否被收藏过
        Long userId = LoginUserContextHolder.getUserId();

        String userNoteCollectListKey = RedisKeyConstants.buildBloomUserNoteCollectListKey(userId);

        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setResultType(Long.class);
        script.setScriptSource(new ResourceScriptSource(new ClassPathResource("/lua/bloom_note_uncollect_check.lua")));

        Long result = redisTemplate.execute(script, Collections.singletonList(userNoteCollectListKey), noteId);

        NoteUnCollectLuaResultEnum resultEnum = NoteUnCollectLuaResultEnum.valueOf(result);
        switch (resultEnum) {
            case NOT_EXIST -> {
                // 查询 DB, 校验笔记是否被收藏, 并异步初始化 bloom
                threadPoolTaskExecutor.submit(() -> {
                    long expireSeconds = 24 * 60 * 60 + RandomUtil.randomInt(60 * 60 * 24);
                    
                    batchAddNoteCollect2BloomAndExpire(noteId, expireSeconds, userNoteCollectListKey);
                });
                
                int count = noteCollectionDOMapper.selectCountByUserIdAndNoteId(userId, noteId);
                if (count == 0) {
                    throw new BizException(ResponseCodeEnum.NOTE_NOT_COLLECTED);
                }
            }
            // 未收藏, 这里判断绝对正确, 不存在误判
            case NOTE_NOT_COLLECTED ->
                    throw new BizException(ResponseCodeEnum.NOTE_NOT_COLLECTED);
        }

        // 3. 删除 zset 已收藏的笔记 ID
        // 能走到这里说明布隆过滤器判断已收藏
        String userNoteCollectZSetKey = RedisKeyConstants.buildUserNoteCollectZSetKey(userId);
        redisTemplate.opsForZSet()
                .remove(userNoteCollectZSetKey, noteId);

        // 4. 走 MQ, 数据入库
        CollectUnCollectNoteMqDTO mqDTO = CollectUnCollectNoteMqDTO.builder()
                .noteId(noteId)
                .userId(userId)
                .type(CollectUnCollectNoteTypeEnum.UN_COLLECT.getCode())
                .noteCreatorId(creatorId)
                .build();

        Message<String> message = MessageBuilder.withPayload(JsonUtils.toJsonString(mqDTO))
                .build();
        
        String destination = MQConstants.TOPIC_COLLECT_OR_UN_COLLECT + ":" + MQConstants.TAG_UN_COLLECT;
        String hashKey = String.valueOf(userId);
        
        rocketMQTemplate.asyncSendOrderly(destination, message, hashKey, new SendCallback() {
            @Override
            public void onSuccess(SendResult sendResult) {
                log.info("==> 【笔记取消收藏】MQ 发送成功, SendResult: {}", sendResult);
            }

            @Override
            public void onException(Throwable throwable) {
                log.error("==> 【笔记取消收藏】MQ 发送异常: ", throwable);
            }
        });

        return Response.success();
    }

    /**
     * 异步初始化用户收藏笔记 ZSET
     *
     * @param userId
     * @param redisKey
     */
    private void asyncInitUserNoteCollectsZSet(Long userId, String redisKey) {
        Boolean exists = redisTemplate.hasKey(redisKey);
        if (!exists) {
            // 查询最新收藏的 300 篇笔记
            List<NoteCollectionDO> noteCollectionDOS = noteCollectionDOMapper.selectCollectedByUserIdAndLimit(userId, 300);
            if (CollUtil.isNotEmpty(noteCollectionDOS)) {
                long expireSeconds = 60 * 60 * 24 + RandomUtil.randomInt(60 * 60 * 24);

                Object[] luaArgs = buildNoteCollectZSetLuaArgs(noteCollectionDOS, expireSeconds);

                DefaultRedisScript<Long> script = new DefaultRedisScript<>();
                script.setResultType(Long.class);
                script.setScriptSource(new ResourceScriptSource(new ClassPathResource("/lua/batch_add_note_collect_zset_and_expire.lua")));

                redisTemplate.execute(script, Collections.singletonList(redisKey), luaArgs);
            }
        }
    }

    /**
     * 初始化笔记收藏布隆过滤器
     *
     * @param userId
     * @param expireSeconds
     * @param redisKey
     */
    private void batchAddNoteCollect2BloomAndExpire(Long userId, long expireSeconds, String redisKey) {

        List<NoteCollectionDO> noteCollectionDOS = noteCollectionDOMapper.selectByUserId(userId);
        if (CollUtil.isNotEmpty(noteCollectionDOS)) {
            try {
                DefaultRedisScript<Long> script = new DefaultRedisScript<>();
                script.setResultType(Long.class);
                script.setScriptSource(new ResourceScriptSource(new ClassPathResource("/lua/bloom_batch_add_note_collect_and_expire.lua")));

                // 构建 Lua 参数
                List<Object> luaArgs = Lists.newArrayList();
                noteCollectionDOS.forEach(noteCollectionDO ->
                        luaArgs.add(noteCollectionDO.getNoteId()));
                luaArgs.add(expireSeconds);

                redisTemplate.execute(script, Collections.singletonList(redisKey), luaArgs.toArray());
            } catch (Exception e) {
                log.error("## 异步初始化【笔记收藏】布隆过滤器异常: {}", e.getMessage(), e);
            }
        }
    }

    /**
     * 初始化 zset
     *
     * @param userId
     * @param key
     */
    private void asyncInitUserNoteLikesZSet(Long userId, String key) {
        threadPoolTaskExecutor.submit(() -> {
            Boolean hasKey = redisTemplate.hasKey(key);
            if (!hasKey) {
                List<NoteLikeDO> noteLikeDOS = noteLikeDOMapper.selectLikedByUserIdAndLimit(userId, 100);
                if (CollUtil.isNotEmpty(noteLikeDOS)) {
                    long expireSeconds = 60 * 60 * 24 + RandomUtil.randomInt(60 * 60 * 24);

                    Object[] luaArgs = buildNoteLikeZSetLuaArgs(noteLikeDOS, expireSeconds);

                    DefaultRedisScript<Long> script = new DefaultRedisScript<>();
                    script.setResultType(Long.class);
                    script.setScriptSource(new ResourceScriptSource(new ClassPathResource("/lua/batch_add_note_like_zset_and_expire.lua")));

                    redisTemplate.execute(script, Collections.singletonList(key), luaArgs);
                }
            }
        });
    }

    /**
     * 构建 lua 脚本参数
     *
     * @param noteCollectionDOS
     * @param expireSeconds
     * @return
     */
    private Object[] buildNoteCollectZSetLuaArgs(List<NoteCollectionDO> noteCollectionDOS, long expireSeconds) {
        int argsLength = noteCollectionDOS.size() * 2 + 1;
        Object[] luaArgs = new Object[argsLength];

        int i = 0;
        for (NoteCollectionDO noteCollectionDO : noteCollectionDOS) {
            luaArgs[i] = DateUtils.localDateTime2Timestamp(noteCollectionDO.getCreateTime());
            luaArgs[i + 1] = noteCollectionDO.getNoteId();
            i += 2;
        }

        luaArgs[argsLength - 1] = expireSeconds;
        return luaArgs;
    }

    /**
     * 构建 lua 脚本参数
     *
     * @param noteLikeDOList
     * @return
     */
    private Object[] buildNoteLikeZSetLuaArgs(List<NoteLikeDO> noteLikeDOList, long expireSeconds) {
        int argsLength = noteLikeDOList.size() * 2 + 1;
        Object[] luaArgs = new Object[argsLength];

        int i = 0;
        for (NoteLikeDO noteLikeDO : noteLikeDOList) {
            luaArgs[i] = DateUtils.localDateTime2Timestamp(noteLikeDO.getCreateTime());
            luaArgs[i + 1] = noteLikeDO.getNoteId();
            i += 2;
        }

        luaArgs[argsLength - 1] = expireSeconds;
        return luaArgs;
    }

    /**
     * 异步初始化布隆过滤器
     *
     * @param userId
     * @param expireSeconds
     * @param redisKey
     */
    private void batchAddNoteLike2BloomAndExpire(Long userId, long expireSeconds, String redisKey) {
        try {
            // 异步全量同步一下, 设置过期时间
            List<NoteLikeDO> noteLikeDOS = noteLikeDOMapper.selectByUserId(userId);
            if (CollUtil.isNotEmpty(noteLikeDOS)) {
                DefaultRedisScript<Long> script = new DefaultRedisScript<>();
                script.setResultType(Long.class);
                script.setScriptSource(new ResourceScriptSource(new ClassPathResource("/lua/bloom_batch_add_note_like_and_expire.lua")));

                List<Object> luaArgs = Lists.newArrayList();
                noteLikeDOS.forEach(noteLikeDO -> luaArgs.add(noteLikeDO.getNoteId()));
                luaArgs.add(expireSeconds);

                redisTemplate.execute(script, Collections.singletonList(redisKey), luaArgs.toArray());
            }
        } catch (Exception e) {
            log.error("## 异步初始化布隆过滤器异常: {}", e.getMessage(), e);
        }
    }

    /**
     * 判断笔记是否存在（先走本地缓存, 再走 redis, 再走 DB）, 如果存在返回该笔记的发布者 ID
     * 如果前两个缓存不存在需要把 DB 数据存入 redis,
     * 在下次获取的时候如果本地缓存还是不存在就会从 redis 存入
     *
     * @param noteId
     */
    private Long checkNoteIsExist(Long noteId) {

        String existedCacheVOStr = LOCAL_CACHE.getIfPresent(noteId);
        FindNoteDetailRspVO vo = JsonUtils.parseObject(existedCacheVOStr, FindNoteDetailRspVO.class);
        if (vo == null) {
            String key = RedisKeyConstants.buildNoteDetailKey(noteId);
            String noteDetailJson = redisTemplate.opsForValue().get(key);
            vo = JsonUtils.parseObject(noteDetailJson, FindNoteDetailRspVO.class);
            if (vo == null) {
                Long creatorId = noteDOMapper.selectCreatorIdByNoteId(noteId);
                if (Objects.isNull(creatorId)) {
                    throw new BizException(ResponseCodeEnum.NOTE_NOT_FOUND);
                }

                // 如果 DB 存在数据, 异步同步缓存
                threadPoolTaskExecutor.submit(() -> {
                    FindNoteDetailReqVO findNoteDetailReqVO = FindNoteDetailReqVO.builder()
                            .id(noteId).build();

                    findNoteDetail(findNoteDetailReqVO);
                });
                
                return creatorId;
            }
        }
        
        return vo.getCreatorId();
    }

    /**
     * 用 broadcast 模式把所有实例的本地缓存都删除掉
     *
     * @param noteId
     */
    private void removeLocalCacheByMQBroadcast(Long noteId) {
        rocketMQTemplate.syncSend(MQConstants.TOPIC_DELETE_NOTE_LOCAL_CACHE, noteId);
        log.info("====> MQ: 删除笔记本地缓存发送成功...");
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
