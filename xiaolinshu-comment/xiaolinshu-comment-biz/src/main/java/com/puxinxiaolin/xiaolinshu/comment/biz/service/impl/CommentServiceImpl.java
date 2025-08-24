package com.puxinxiaolin.xiaolinshu.comment.biz.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.RandomUtil;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.puxinxiaolin.framework.biz.context.holder.LoginUserContextHolder;
import com.puxinxiaolin.framework.common.constant.DateConstants;
import com.puxinxiaolin.framework.common.exception.BizException;
import com.puxinxiaolin.framework.common.response.PageResponse;
import com.puxinxiaolin.framework.common.response.Response;
import com.puxinxiaolin.framework.common.util.DateUtils;
import com.puxinxiaolin.framework.common.util.JsonUtils;
import com.puxinxiaolin.xiaolinshu.comment.biz.constant.MQConstants;
import com.puxinxiaolin.xiaolinshu.comment.biz.constant.RedisKeyConstants;
import com.puxinxiaolin.xiaolinshu.comment.biz.domain.dataobject.CommentDO;
import com.puxinxiaolin.xiaolinshu.comment.biz.domain.mapper.CommentDOMapper;
import com.puxinxiaolin.xiaolinshu.comment.biz.domain.mapper.NoteCountDOMapper;
import com.puxinxiaolin.xiaolinshu.comment.biz.enums.ResponseCodeEnum;
import com.puxinxiaolin.xiaolinshu.comment.biz.model.dto.PublishCommentMqDTO;
import com.puxinxiaolin.xiaolinshu.comment.biz.model.vo.FindCommentItemRspVO;
import com.puxinxiaolin.xiaolinshu.comment.biz.model.vo.FindCommentPageListReqVO;
import com.puxinxiaolin.xiaolinshu.comment.biz.model.vo.PublishCommentReqVO;
import com.puxinxiaolin.xiaolinshu.comment.biz.retry.SendMqRetryHelper;
import com.puxinxiaolin.xiaolinshu.comment.biz.rpc.DistributedIdGeneratorRpcService;
import com.puxinxiaolin.xiaolinshu.comment.biz.rpc.KeyValueRpcService;
import com.puxinxiaolin.xiaolinshu.comment.biz.rpc.UserRpcService;
import com.puxinxiaolin.xiaolinshu.comment.biz.service.CommentService;
import com.puxinxiaolin.xiaolinshu.kv.api.dto.req.FindCommentContentReqDTO;
import com.puxinxiaolin.xiaolinshu.kv.api.dto.rsp.FindCommentContentRspDTO;
import com.puxinxiaolin.xiaolinshu.user.api.dto.resp.FindUserByIdRspDTO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.bouncycastle.util.Strings;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.springframework.data.redis.core.*;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@Slf4j
public class CommentServiceImpl implements CommentService {
    @Resource
    private DistributedIdGeneratorRpcService distributedIdGeneratorRpcService;
    @Resource
    private UserRpcService userRpcService;
    @Resource
    private KeyValueRpcService keyValueRpcService;
    @Resource
    private NoteCountDOMapper noteCountDOMapper;
    @Resource
    private CommentDOMapper commentDOMapper;
    @Resource
    private SendMqRetryHelper sendMqRetryHelper;
    @Resource
    private RedisTemplate<String, Object> redisTemplate;
    @Resource(name = "taskExecutor")
    private ThreadPoolTaskExecutor threadPoolTaskExecutor;

    private static final Cache<Long, String> LOCAL_CACHE = Caffeine.newBuilder()
            .initialCapacity(10000)
            .maximumSize(10000)
            .expireAfterWrite(1, TimeUnit.HOURS)
            .build();

    /**
     * 评论列表分页查询
     *
     * @param request
     * @return
     */
    @Override
    public PageResponse<FindCommentItemRspVO> findCommentPageList(FindCommentPageListReqVO request) {
        Long noteId = request.getNoteId();
        Integer pageNo = request.getPageNo();
        long pageSize = 10;

        // 先走缓存, 缓存不存在走 DB
        String noteCommentTotalKey = RedisKeyConstants.buildNoteCommentTotalKey(noteId);
        Number commentTotal = (Number) redisTemplate.opsForHash()
                .get(noteCommentTotalKey, RedisKeyConstants.FIELD_COMMENT_TOTAL);
        long count = Objects.isNull(commentTotal) ? 0 : commentTotal.longValue();
        if (Objects.isNull(commentTotal)) {
            // 查询评论总数 (从 t_note_count 笔记计数表查, 提升查询性能, 避免 count(*))
            Long dbCount = noteCountDOMapper.selectCommentTotalByNoteId(noteId);
            if (Objects.isNull(dbCount)) {
                throw new BizException(ResponseCodeEnum.COMMENT_NOT_FOUND);
            }

            count = dbCount;
            threadPoolTaskExecutor.submit(() ->
                    syncNoteCommentTotal2Redis(noteCommentTotalKey, dbCount)
            );
        }

        // 若查 DB 的评论总数为 0, 则直接响应
        if (count == 0) {
            return PageResponse.success(null, pageNo, 0);
        }

        List<FindCommentItemRspVO> result = null;
        if (count > 0) {
            result = Lists.newArrayList();

            long offset = PageResponse.getOffset(pageNo, pageSize);

            // 先走 redis
            String commentZSetKey = RedisKeyConstants.buildCommentListKey(noteId);
            Boolean hasKey = redisTemplate.hasKey(commentZSetKey);
            // 如果缓存不存在, 则同步数据到 redis
            if (!hasKey) {
                threadPoolTaskExecutor.submit(() ->
                        syncHeatComments2Redis(commentZSetKey, noteId)
                );
            }
            // 缓存存在, 并且查询的是前 50 页的评论
            if (hasKey && offset < 500) {
                Set<Object> commentIds = redisTemplate.opsForZSet()
                        .reverseRangeByScore(commentZSetKey, -Double.MAX_VALUE, Double.MAX_VALUE, offset, pageSize);
                if (CollUtil.isNotEmpty(commentIds)) {
                    List<Object> commentIdList = Lists.newArrayList(commentIds);

                    // 1. 先查本地缓存
                    // 本地缓存中不存在的评论 ID
                    List<Long> localCacheExpireCommentIds = Lists.newArrayList();
                    List<Long> localCacheKeys = commentIdList.stream()
                            .map(commentId -> Long.valueOf(commentId.toString()))
                            .toList();
                    // getAll(param1, param2): 第二个参数是回调函数, 用于处理缓存中不存在的 key
                    Map<Long, String> commentIdAndDetailJsonMap = LOCAL_CACHE.getAll(localCacheKeys, missingKeys -> {
                        Map<Long, String> missingData = Maps.newHashMap();
                        missingKeys.forEach(key -> {
                            localCacheExpireCommentIds.add(key);
                            missingData.put(key, "");
                        });

                        return missingData;
                    });
                    
                    // 说明本地缓存有部分数据, 先加到返参中
                    if (localCacheExpireCommentIds.size() != commentIdList.size()) {
                        for (String value : commentIdAndDetailJsonMap.values()) {
                            if (StringUtils.isBlank(value)) continue;
                            
                            FindCommentItemRspVO commentRspVO = JsonUtils.parseObject(value, FindCommentItemRspVO.class);
                            result.add(commentRspVO);
                        }
                    }
                    // 说明数据都在本地缓存, 没有缺失值
                    if (localCacheExpireCommentIds.isEmpty()) {
                        return PageResponse.success(result, pageNo, count, pageSize);
                    }

                    // 2. 构建 MGET 的 keys, 从 redis 拿评论详情
                    List<String> commentIdKeys = commentIdList.stream()
                            .map(RedisKeyConstants::buildCommentDetailKey)
                            .toList();
                    List<Object> commentJsonList = redisTemplate.opsForValue()
                            .multiGet(commentIdKeys);

                    // 可能存在部分评论不在缓存中, 已经过期被删除, 这些评论 ID 需要提取出来, 等会查数据库
                    List<Long> expiredCommentIds = Lists.newArrayList();
                    for (int i = 0; i < commentJsonList.size(); i++) {
                        String commentJson = (String) commentJsonList.get(i);
                        // 缓存中存在的评论 Json, 直接转换为 VO 添加到返参集合中
                        if (Objects.nonNull(commentJson)) {
                            FindCommentItemRspVO commentItemRspVO = JsonUtils.parseObject(commentJson, FindCommentItemRspVO.class);
                            result.add(commentItemRspVO);
                        } else {
                            expiredCommentIds.add(Long.valueOf(commentIdList.get(i).toString()));
                        }
                    }

                    // 对于不存在的一级评论, 需要批量从数据库中查询, 添加到 result 中并同步到 redis
                    if (CollUtil.isNotEmpty(expiredCommentIds)) {
                        List<CommentDO> commentDOS = commentDOMapper.selectByCommentIds(expiredCommentIds);
                        getCommentDataAndSync2Redis(commentDOS, noteId, result);
                    }
                }

                // 按热度值降序
                result = result.stream()
                        .sorted(Comparator.comparing(FindCommentItemRspVO::getHeat).reversed())
                        .collect(Collectors.toList());

                // 添加到本地缓存 
                syncCommentDetail2LocalCache(result);

                return PageResponse.success(result, pageNo, count, pageSize);
            }

            List<CommentDO> oneLevelCommentDOS = commentDOMapper.selectPageList(noteId, offset, pageSize);
            // 获取全部评论数据, 并将评论详情同步到 Redis 中
            getCommentDataAndSync2Redis(oneLevelCommentDOS, noteId, result);

            // 添加到本地缓存
            syncCommentDetail2LocalCache(result);
        }
        return PageResponse.success(result, pageNo, count, pageSize);
    }

    /**
     * 发布评论（由于该接口是高并发接口, 这里发 MQ 来实现异步写）
     *
     * @param request
     * @return
     */
    @Override
    public Response<?> publishComment(PublishCommentReqVO request) {
        String content = request.getContent();
        String imageUrl = request.getImageUrl();

        Preconditions.checkArgument(StringUtils.isNotBlank(content) || StringUtils.isNotBlank(imageUrl),
                "评论正文和图片不能同时为空");

        Long creatorId = LoginUserContextHolder.getUserId();
        String commentId = distributedIdGeneratorRpcService.generateCommentId();
        PublishCommentMqDTO mqDTO = PublishCommentMqDTO.builder()
                .noteId(request.getNoteId())
                .content(content)
                .imageUrl(imageUrl)
                .commentId(Long.valueOf(commentId))
                .replyCommentId(request.getReplyCommentId())
                .createTime(LocalDateTime.now())
                .creatorId(creatorId)
                .build();

        // 走 MQ（包括重试机制）
        sendMqRetryHelper.asyncSend(MQConstants.TOPIC_PUBLISH_COMMENT, JsonUtils.toJsonString(mqDTO));

        return Response.success();
    }

    /**
     * 同步评论详情到本地缓存
     *
     * @param result
     */
    private void syncCommentDetail2LocalCache(List<FindCommentItemRspVO> result) {
        // 异步批量写
        threadPoolTaskExecutor.submit(() -> {
            Map<Long, String> localCacheData = Maps.newHashMap();
            result.forEach(vo -> {
                Long commentId = vo.getCommentId();
                localCacheData.put(commentId, JsonUtils.toJsonString(vo));
            });

            LOCAL_CACHE.putAll(localCacheData);
        });
    }

    /**
     * 获取全部评论数据, 并将评论详情同步到 Redis 中
     *
     * @param oneLevelCommentDOS
     * @param noteId
     * @param result
     */
    private void getCommentDataAndSync2Redis(List<CommentDO> oneLevelCommentDOS, Long noteId,
                                             List<FindCommentItemRspVO> result) {
        // 过滤出所有最早回复的二级评论 ID
        List<Long> twoLevelCommentIds = oneLevelCommentDOS.stream()
                .map(CommentDO::getFirstReplyCommentId)
                .filter(replyCommentId -> replyCommentId != 0)
                .toList();

        // Map<二级评论 ID, 二级评论对应的 CommentDO>
        Map<Long, CommentDO> commentIdAndDOMap = null;
        List<CommentDO> twoLevelCommentDOS = null;
        if (CollUtil.isNotEmpty(twoLevelCommentIds)) {
            twoLevelCommentDOS = commentDOMapper.selectTwoLevelCommentByIds(twoLevelCommentIds);

            commentIdAndDOMap = twoLevelCommentDOS.stream()
                    .collect(Collectors.toMap(CommentDO::getId, commentDO -> commentDO));
        }

        // 走 RPC
        // 准备 kv、user 入参
        List<FindCommentContentReqDTO> findCommentContentReqDTOS = Lists.newArrayList();
        List<Long> userIds = Lists.newArrayList();
        // 合并一级评论和二级评论
        List<CommentDO> allCommentDOS = Lists.newArrayList();
        CollUtil.addAll(allCommentDOS, oneLevelCommentDOS);
        CollUtil.addAll(allCommentDOS, twoLevelCommentDOS);
        allCommentDOS.forEach(commentDO -> {
            Boolean isContentEmpty = commentDO.getIsContentEmpty();
            if (!isContentEmpty) {
                FindCommentContentReqDTO dto = FindCommentContentReqDTO.builder()
                        .contentId(commentDO.getContentUuid())
                        .yearMonth(DateConstants.Y_M.format(commentDO.getCreateTime()))
                        .build();
                findCommentContentReqDTOS.add(dto);
            }

            userIds.add(commentDO.getUserId());
        });

        // kv-RPC
        List<FindCommentContentRspDTO> commentContentRspDTOS = keyValueRpcService.batchFindCommentContent(noteId, findCommentContentReqDTOS);
        Map<String, String> commentUuidAndContentMap = null;
        if (CollUtil.isNotEmpty(commentContentRspDTOS)) {
            commentUuidAndContentMap = commentContentRspDTOS.stream()
                    .collect(Collectors.toMap(FindCommentContentRspDTO::getContentId, FindCommentContentRspDTO::getContent));
        }

        // user-RPC
        List<FindUserByIdRspDTO> findUserByIdRspDTOS = userRpcService.findByIds(userIds);
        Map<Long, FindUserByIdRspDTO> userIdAndDTOMap = null;
        if (CollUtil.isNotEmpty(findUserByIdRspDTOS)) {
            userIdAndDTOMap = findUserByIdRspDTOS.stream()
                    .collect(Collectors.toMap(FindUserByIdRspDTO::getId, dto -> dto));
        }

        // 拼接参数
        for (CommentDO oneCommentDO : oneLevelCommentDOS) {
            Long userId = oneCommentDO.getUserId();
            // 一级评论
            FindCommentItemRspVO oneLevelCommentRspVO = FindCommentItemRspVO.builder()
                    .userId(userId)
                    .commentId(oneCommentDO.getId())
                    .imageUrl(oneCommentDO.getImageUrl())
                    .createTime(DateUtils.formatRelativeTime(oneCommentDO.getCreateTime()))
                    .likeTotal(oneCommentDO.getLikeTotal())
                    .childCommentTotal(oneCommentDO.getChildCommentTotal())
                    .heat(oneCommentDO.getHeat())
                    .build();

            // 设置用户和评论信息
            setUserInfo(oneLevelCommentRspVO, userIdAndDTOMap, userId, commentIdAndDOMap);
            setCommentContent(oneLevelCommentRspVO, commentUuidAndContentMap, oneCommentDO);

            // 二级评论
            Long firstReplyCommentId = oneCommentDO.getFirstReplyCommentId();
            if (CollUtil.isNotEmpty(commentIdAndDOMap)) {
                CommentDO twoLevelCommentDO = commentIdAndDOMap.get(firstReplyCommentId);
                if (Objects.nonNull(twoLevelCommentDO)) {
                    Long twoLevelCommentUserId = twoLevelCommentDO.getUserId();
                    FindCommentItemRspVO twoLevelCommentRspVO = FindCommentItemRspVO.builder()
                            .userId(twoLevelCommentUserId)
                            .likeTotal(twoLevelCommentDO.getLikeTotal())
                            .commentId(twoLevelCommentDO.getId())
                            .heat(twoLevelCommentDO.getHeat())
                            .imageUrl(twoLevelCommentDO.getImageUrl())
                            .createTime(DateUtils.formatRelativeTime(twoLevelCommentDO.getCreateTime()))
                            .build();

                    setUserInfo(twoLevelCommentRspVO, userIdAndDTOMap, twoLevelCommentUserId, commentIdAndDOMap);
                    setCommentContent(twoLevelCommentRspVO, commentUuidAndContentMap, twoLevelCommentDO);

                    oneLevelCommentRspVO.setFirstReplyComment(twoLevelCommentRspVO);
                }
            }
            result.add(oneLevelCommentRspVO);

            // 同步笔记详情到 redis
            threadPoolTaskExecutor.submit(() -> {
                Map<String, String> data = Maps.newHashMap();

                result.forEach(vo -> {
                    Long commentId = vo.getCommentId();
                    String key = RedisKeyConstants.buildCommentDetailKey(commentId);
                    data.put(key, JsonUtils.toJsonString(vo));
                });

                redisTemplate.executePipelined((RedisCallback<?>) connection -> {
                    for (Map.Entry<String, String> entry : data.entrySet()) {
                        int randomExpire = RandomUtil.randomInt(60 * 60 * 5);

                        connection.setEx(
                                redisTemplate.getStringSerializer().serialize(entry.getKey()),
                                randomExpire,
                                redisTemplate.getStringSerializer().serialize(entry.getValue())
                        );
                    }

                    return null;
                });
            });
        }
    }

    /**
     * 同步热点评论至 Redis
     *
     * @param commentZSetKey
     * @param noteId
     */
    private void syncHeatComments2Redis(String commentZSetKey, Long noteId) {
        List<CommentDO> commentDOS = commentDOMapper.selectHeatComments(noteId);
        if (CollUtil.isNotEmpty(commentDOS)) {
            redisTemplate.executePipelined((RedisCallback<?>) connection -> {
                ZSetOperations<String, Object> zSetOps = redisTemplate.opsForZSet();

                for (CommentDO commentDO : commentDOS) {
                    Long id = commentDO.getId();
                    Double heat = commentDO.getHeat();

                    zSetOps.add(commentZSetKey, id, heat);
                }

                // 随机设置过期时间
                long expireSeconds = RandomUtil.randomInt(60 * 60 * 5);
                redisTemplate.expire(commentZSetKey, expireSeconds, TimeUnit.SECONDS);

                return null;
            });
        }
    }

    /**
     * 同步笔记评论总数到 Redis
     *
     * @param noteCommentTotalKey
     * @param dbCount
     */
    private void syncNoteCommentTotal2Redis(String noteCommentTotalKey, Long dbCount) {
        redisTemplate.executePipelined(new SessionCallback<>() {
            @Override
            public Object execute(RedisOperations operations) {
                operations.opsForHash()
                        .put(noteCommentTotalKey, RedisKeyConstants.FIELD_COMMENT_TOTAL, dbCount);

                // 随机设置过期时间
                long expireSeconds = 60 * 60 + RandomUtil.randomInt(60 * 60 * 4);
                operations.expire(noteCommentTotalKey, expireSeconds, TimeUnit.SECONDS);

                return null;
            }
        });
    }

    /**
     * 设置用户信息
     *
     * @param findCommentItemRspVO
     * @param userIdAndDTOMap
     * @param userId
     * @param commentIdAndDOMap    todo: 看是否需要删除该无用参数
     */
    private void setUserInfo(FindCommentItemRspVO findCommentItemRspVO,
                             Map<Long, FindUserByIdRspDTO> userIdAndDTOMap,
                             Long userId, Map<Long, CommentDO> commentIdAndDOMap) {
        if (CollUtil.isNotEmpty(userIdAndDTOMap)) {
            FindUserByIdRspDTO findUserByIdRspDTO = userIdAndDTOMap.get(userId);
            if (Objects.nonNull(findUserByIdRspDTO)) {
                findCommentItemRspVO.setAvatar(findUserByIdRspDTO.getAvatar());
                findCommentItemRspVO.setNickname(findUserByIdRspDTO.getNickName());
            }
        }
    }

    /**
     * 设置评论内容
     *
     * @param findCommentItemRspVO
     * @param commentUuidAndContentMap
     * @param commentDO
     */
    private void setCommentContent(FindCommentItemRspVO findCommentItemRspVO,
                                   Map<String, String> commentUuidAndContentMap,
                                   CommentDO commentDO) {
        if (CollUtil.isNotEmpty(commentUuidAndContentMap)) {
            String contentUuid = commentDO.getContentUuid();
            if (StringUtils.isNotBlank(contentUuid)) {
                findCommentItemRspVO.setContent(commentUuidAndContentMap.get(contentUuid));
            }
        }
    }

}
