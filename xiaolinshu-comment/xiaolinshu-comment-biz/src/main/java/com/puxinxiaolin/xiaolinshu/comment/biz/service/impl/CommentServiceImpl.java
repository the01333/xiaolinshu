package com.puxinxiaolin.xiaolinshu.comment.biz.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.RandomUtil;
import com.alibaba.nacos.shaded.com.google.common.collect.Sets;
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
import com.puxinxiaolin.xiaolinshu.comment.biz.domain.dataobject.CommentLikeDO;
import com.puxinxiaolin.xiaolinshu.comment.biz.domain.mapper.CommentDOMapper;
import com.puxinxiaolin.xiaolinshu.comment.biz.domain.mapper.CommentLikeDOMapper;
import com.puxinxiaolin.xiaolinshu.comment.biz.domain.mapper.NoteCountDOMapper;
import com.puxinxiaolin.xiaolinshu.comment.biz.enums.*;
import com.puxinxiaolin.xiaolinshu.comment.biz.model.dto.DeleteCommentReqVO;
import com.puxinxiaolin.xiaolinshu.comment.biz.model.dto.LikeUnlikeCommentMqDTO;
import com.puxinxiaolin.xiaolinshu.comment.biz.model.dto.PublishCommentMqDTO;
import com.puxinxiaolin.xiaolinshu.comment.biz.model.vo.*;
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
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.units.qual.K;
import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.*;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

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
    private CommentLikeDOMapper commentLikeDOMapper;
    @Resource
    private SendMqRetryHelper sendMqRetryHelper;
    @Resource
    private RedisTemplate<String, Object> redisTemplate;
    @Resource
    private RocketMQTemplate rocketMQTemplate;
    @Resource(name = "taskExecutor")
    private ThreadPoolTaskExecutor threadPoolTaskExecutor;
    @Resource
    private TransactionTemplate transactionTemplate;

    private static final Cache<Long, String> LOCAL_CACHE = Caffeine.newBuilder()
            .initialCapacity(10000)
            .maximumSize(10000)
            .expireAfterWrite(1, TimeUnit.HOURS)
            .build();

    /**
     * 删除评论本地缓存
     *
     * @param commentId
     */
    @Override
    public void deleteCommentLocalCache(Long commentId) {
        LOCAL_CACHE.invalidate(commentId);
    }

    /**
     * 删除评论
     *
     * @param request
     * @return
     */
    @Override
    public Response<?> deleteComment(DeleteCommentReqVO request) {
        Long commentId = request.getCommentId();

        // 1. 校验评论是否存在
        CommentDO commentDO = commentDOMapper.selectByPrimaryKey(commentId);
        if (Objects.isNull(commentDO)) {
            throw new BizException(ResponseCodeEnum.COMMENT_NOT_FOUND);
        }

        // 2. 校验是否有权限删除
        Long userId = LoginUserContextHolder.getUserId();
        if (!Objects.equals(userId, commentDO.getUserId())) {
            throw new BizException(ResponseCodeEnum.COMMENT_CANT_OPERATE);
        }

        // 3. 物理删除评论、评论内容（调用 KV 模块）
        transactionTemplate.execute(status -> {
            try {
                commentDOMapper.deleteByPrimaryKey(commentId);
                keyValueRpcService.deleteCommentContent(commentDO.getNoteId(), commentDO.getCreateTime(), commentDO.getContentUuid());

                return null;
            } catch (Exception e) {
                status.setRollbackOnly();
                log.error("", e);
                // 考虑到 RPC 可能调用失败, 这里重新抛出异常, 让调用者能够感知到异常的发生
                throw e;
            }
        });

        // 4. 删除 redis 缓存
        Integer level = commentDO.getLevel();
        Long noteId = commentDO.getNoteId();
        Long parentCommentId = commentDO.getParentId();
        String redisZSetKey = Objects.equals(level, CommentLevelEnum.ONE.getCode()) ?
                RedisKeyConstants.buildCommentListKey(noteId) :
                RedisKeyConstants.buildChildCommentListKey(parentCommentId);

        redisTemplate.executePipelined(new SessionCallback<>() {
            @Override
            public Object execute(RedisOperations operations) {
                // 删除 ZSet 中的评论 ID
                operations.opsForZSet().remove(redisZSetKey, commentId);
                // 删除评论详情
                operations.delete(RedisKeyConstants.buildCommentDetailKey(commentId));
                return null;
            }
        });

        // 5. 走广播 MQ, 删除本地缓存
        rocketMQTemplate.asyncSend(MQConstants.TOPIC_DELETE_COMMENT_LOCAL_CACHE, commentId, new SendCallback() {
            @Override
            public void onSuccess(SendResult sendResult) {
                log.info("==> 【删除评论详情本地缓存】MQ 发送成功, SendResult: {}", sendResult);
            }

            @Override
            public void onException(Throwable e) {
                log.error("==> 【删除评论详情本地缓存】MQ 发送异常: ", e);
            }
        });

        // 6. 走 MQ, 异步更新计数、删除关联评论、热度值等
        Message<String> message = MessageBuilder.withPayload(JsonUtils.toJsonString(commentDO))
                .build();
        rocketMQTemplate.asyncSend(MQConstants.TOPIC_DELETE_COMMENT, message, new SendCallback() {
            @Override
            public void onSuccess(SendResult sendResult) {
                log.info("==> 【评论删除】MQ 发送成功, SendResult: {}", sendResult);
            }

            @Override
            public void onException(Throwable e) {
                log.error("==> 【评论删除】MQ 发送异常: ", e);
            }
        });

        return Response.success();
    }

    /**
     * 取消评论点赞
     *
     * @param request
     * @return
     */
    @Override
    public Response<?> unlikeComment(UnLikeCommentReqVO request) {
        Long commentId = request.getCommentId();

        // 1. 校验评论是否存在
        checkCommentIsExist(commentId);

        // 2. 校验评论是否被点赞过
        Long userId = LoginUserContextHolder.getUserId();
        String redisKey = RedisKeyConstants.buildBloomCommentLikesKey(userId);

        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setResultType(Long.class);
        script.setScriptSource(new ResourceScriptSource(new ClassPathResource("/lua/bloom_comment_unlike_check.lua")));

        Long result = redisTemplate.execute(script, Collections.singletonList(redisKey), commentId);
        CommentUnlikeLuaResultEnum commentUnlikeLuaResultEnum = CommentUnlikeLuaResultEnum.valueOf(result);
        if (commentUnlikeLuaResultEnum == null) {
            return Response.fail(ResponseCodeEnum.PARAM_NOT_VALID);
        }

        switch (commentUnlikeLuaResultEnum) {
            // 布隆过滤器不存在, 异步初始化
            case NOT_EXIST -> {
                threadPoolTaskExecutor.submit(() -> {
                    int expireSeconds = 60 * 60 + RandomUtil.randomInt(60 * 60);
                    batchAddCommentLike2BloomAndExpire(userId, expireSeconds, redisKey);
                });

                int count = commentDOMapper.selectCountByUserIdAndCommentId(userId, commentId);
                if (count == 0) {
                    throw new BizException(ResponseCodeEnum.COMMENT_NOT_LIKED);
                }
            }
            // 布隆过滤器校验目标评论未被点赞（判断绝对正确）
            case COMMENT_NOT_LIKED -> throw new BizException(ResponseCodeEnum.COMMENT_NOT_LIKED);
        }

        // 3. 发送顺序 MQ, 删除评论点赞记录
        LikeUnlikeCommentMqDTO mqDTO = LikeUnlikeCommentMqDTO.builder()
                .commentId(commentId)
                .userId(userId)
                .type(LikeUnlikeCommentTypeEnum.UNLIKE.getCode())
                .createTime(LocalDateTime.now())
                .build();

        Message<String> message = MessageBuilder.withPayload(JsonUtils.toJsonString(mqDTO))
                .build();

        String destination = MQConstants.TOPIC_COMMENT_LIKE_OR_UNLIKE + ":" + MQConstants.TAG_UNLIKE;
        String hashKey = String.valueOf(userId);

        rocketMQTemplate.asyncSendOrderly(destination, message, hashKey, new SendCallback() {
            @Override
            public void onSuccess(SendResult sendResult) {
                log.info("==> 【评论取消点赞】MQ 发送成功, SendResult: {}", sendResult);
            }

            @Override
            public void onException(Throwable e) {
                log.error("==> 【评论取消点赞】MQ 发送异常: ", e);
            }
        });

        return Response.success();
    }

    /**
     * 评论点赞
     *
     * @param request
     * @return
     */
    @Override
    public Response<?> likeComment(LikeCommentReqVO request) {
        Long commentId = request.getCommentId();

        // 1. 校验被点赞的评论是否存在
        checkCommentIsExist(commentId);

        // 2. 判断是否已经点过赞
        Long userId = LoginUserContextHolder.getUserId();
        String bloomKey = RedisKeyConstants.buildBloomCommentLikesKey(userId);

        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setResultType(Long.class);
        script.setScriptSource(new ResourceScriptSource(new ClassPathResource("/lua/bloom_comment_like_check.lua")));

        Long result = redisTemplate.execute(script, Collections.singletonList(bloomKey), commentId);
        CommentLikeLuaResultEnum commentLikeLuaResultEnum = CommentLikeLuaResultEnum.valueOf(result);
        if (Objects.isNull(commentLikeLuaResultEnum)) {
            return Response.fail(ResponseCodeEnum.PARAM_NOT_VALID);
        }

        switch (commentLikeLuaResultEnum) {
            // 布隆过滤器不存在
            case NOT_EXIST -> {
                // 从数据库获取评论是否被点赞, 并异步初始化 bloom 过滤器, 带过期时间
                int count = commentLikeDOMapper.selectCountByUserIdAndCommentId(userId, commentId);
                // 保底 1 小小时 + 随机秒数
                long expireSeconds = 60 * 60 + RandomUtil.randomInt(60 * 60);
                if (count > 0) {
                    // 异步初始化 bloom 过滤器, 带过期时间
                    threadPoolTaskExecutor.submit(() -> {
                        batchAddCommentLike2BloomAndExpire(userId, expireSeconds, bloomKey);
                    });

                    throw new BizException(ResponseCodeEnum.COMMENT_ALREADY_LIKED);
                }

                // 若目标评论未被点赞, 查询当前用户是否有点赞其他评论, 有则同步初始化布隆过滤器
                batchAddCommentLike2BloomAndExpire(userId, expireSeconds, bloomKey);

                // 添加当前点赞评论 ID 到布隆过滤器中（先进缓存, 下一次如果出现不存在的评论再统一由上面刷到数据库）
                script.setScriptSource(new ResourceScriptSource(new ClassPathResource("/lua/bloom_add_comment_like_and_expire.lua")));
                script.setResultType(Long.class);
                redisTemplate.execute(script, Collections.singletonList(bloomKey), commentId, expireSeconds);
            }
            // 已经点赞（可能存在误判, 需要进一步确认）
            case COMMENT_LIKED -> {
                int count = commentLikeDOMapper.selectCountByUserIdAndCommentId(userId, commentId);
                if (count > 0) {
                    throw new BizException(ResponseCodeEnum.COMMENT_ALREADY_LIKED);
                }
            }
        }

        // 3. 走 MQ, 入库
        LikeUnlikeCommentMqDTO mqDTO = LikeUnlikeCommentMqDTO.builder()
                .userId(userId)
                .commentId(commentId)
                .type(LikeUnlikeCommentTypeEnum.LIKE.getCode())
                .createTime(LocalDateTime.now())
                .build();
        Message<String> message = MessageBuilder.withPayload(JsonUtils.toJsonString(mqDTO))
                .build();

        String destination = MQConstants.TOPIC_COMMENT_LIKE_OR_UNLIKE + ":" + MQConstants.TAG_LIKE;
        // MQ 分区键
        String hashKey = String.valueOf(userId);

        rocketMQTemplate.asyncSendOrderly(destination, message, hashKey, new SendCallback() {
            @Override
            public void onSuccess(SendResult sendResult) {
                log.info("==> 【评论点赞】MQ 发送成功, SendResult: {}", sendResult);
            }

            @Override
            public void onException(Throwable e) {
                log.error("==> 【评论点赞】MQ 发送异常: ", e);
            }
        });

        return Response.success();
    }

    /**
     * 二级评论分页查询
     *
     * @param request
     * @return
     */
    @Override
    public PageResponse<FindChildCommentPageListRspVO> findChildCommentPageList(FindChildCommentPageListReqVO request) {
        Long parentCommentId = request.getParentCommentId();
        Integer pageNo = request.getPageNo();
        long pageSize = 6;

        // 先走缓存
        String hashKey = RedisKeyConstants.buildCountCommentKey(parentCommentId);
        Number redisCount = (Number) redisTemplate.opsForHash()
                .get(hashKey, RedisKeyConstants.FIELD_CHILD_COMMENT_TOTAL);
        long count = Objects.isNull(redisCount) ? 0L : redisCount.longValue();
        if (Objects.isNull(redisCount)) {
            Long dbCount = commentDOMapper.selectChildCommentTotalById(parentCommentId);
            if (Objects.nonNull(dbCount)) {
                throw new BizException(ResponseCodeEnum.PARENT_COMMENT_NOT_FOUND);
            }

            count = dbCount;
            threadPoolTaskExecutor.execute(() -> {
                syncCommentCount2Redis(hashKey, dbCount);
            });
        }

        if (count == 0) {
            return PageResponse.success(null, pageNo, 0);
        }

        List<FindChildCommentPageListRspVO> result = Lists.newArrayList();

        // 计算分页查询的偏移量 offset (需要 +1, 因为最早回复的二级评论已经被展示了)
        long offset = PageResponse.getOffset(pageNo, pageSize) + 1;

        // 子评论分页缓存使用 ZSET + STRING 实现, 一页 6 条子评论，最多存储 10 页，即 60 条子评论
        String zSetKey = RedisKeyConstants.buildChildCommentListKey(parentCommentId);
        Boolean hasKey = redisTemplate.hasKey(zSetKey);
        if (!hasKey) {
            // 异步将子评论同步到 Redis 中（最多同步 6 * 10 条）
            threadPoolTaskExecutor.execute(() -> {
                syncChildComment2Redis(parentCommentId, zSetKey);
            });
        }
        // 若子评论 ZSET 缓存存在, 并且查询的是前 10 页的子评论
        if (hasKey && offset < 6 * 10) {
            // 按回复时间升序排列
            Set<Object> childCommentIds = redisTemplate.opsForZSet()
                    .rangeByScore(zSetKey, 0, Double.MAX_VALUE, offset, pageSize);
            if (CollUtil.isNotEmpty(childCommentIds)) {
                List<Object> childCommentIdList = Lists.newArrayList();

                // 构建 MGET 批量查询子评论详情的 Key 集合
                List<String> commentIdKeys = childCommentIds.stream()
                        .map(RedisKeyConstants::buildCommentDetailKey)
                        .toList();
                List<Object> commentsJsonList = redisTemplate.opsForValue().multiGet(commentIdKeys);

                // 可能存在部分评论不在缓存中，已经过期被删除，这些评论 ID 需要提取出来，等会查数据库
                List<Long> expiredChildCommentIds = Lists.newArrayList();
                for (int i = 0; i < commentsJsonList.size(); i++) {
                    String commentJson = (String) commentsJsonList.get(i);
                    Long commentId = Long.valueOf(commentsJsonList.get(i).toString());
                    if (Objects.nonNull(commentJson)) {
                        FindChildCommentPageListRspVO rspVO = JsonUtils.parseObject(commentJson, FindChildCommentPageListRspVO.class);
                        result.add(rspVO);
                    } else {
                        expiredChildCommentIds.add(commentId);
                    }
                }

                // 对于缓存中存在的子评论, 需要再次查询 Hash, 获取其计数数据
                if (CollUtil.isNotEmpty(result)) {
                    setChildCommentCountData(result, expiredChildCommentIds);
                }

                // 对于不存在的子评论，需要批量从数据库中查询，并添加到 commentRspVOS 中
                if (CollUtil.isNotEmpty(expiredChildCommentIds)) {
                    List<CommentDO> childCommentDOS = commentDOMapper.selectByCommentIds(expiredChildCommentIds);
                    getChildCommentDataAndSync2Redis(childCommentDOS, result);
                }

                // 按评论 ID 升序排列（等同于按回复时间升序）
                result = result.stream()
                        .sorted(Comparator.comparing(FindChildCommentPageListRspVO::getCommentId))
                        .collect(Collectors.toList());
                return PageResponse.success(result, pageNo, count, pageSize);
            }
        }

        List<CommentDO> childCommentDOS = commentDOMapper.selectChildPageList(parentCommentId, offset, pageSize);
        getChildCommentDataAndSync2Redis(childCommentDOS, result);

        return PageResponse.success(result, pageNo, count, pageSize);
    }

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
                        if (CollUtil.isNotEmpty(result)) {
                            setCommentCountData(result, localCacheExpireCommentIds);
                        }

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
                            // 评论失效需要添加到 expiredCommentIds
                            expiredCommentIds.add(Long.valueOf(commentIdList.get(i).toString()));
                        }
                    }

                    // 对于缓存中存在的评论, 需要再次查询其计数数据
                    if (CollUtil.isNotEmpty(result)) {
                        setCommentCountData(result, expiredCommentIds);
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

            // 从 DB 获取全部评论数据, 并将评论详情同步到 Redis 中
            List<CommentDO> oneLevelCommentDOS = commentDOMapper.selectPageList(noteId, offset, pageSize);
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
     * 批量添加点赞数据到布隆过滤器并设置过期时间
     *
     * @param userId
     * @param expireSeconds
     * @param bloomKey
     */
    private void batchAddCommentLike2BloomAndExpire(Long userId, long expireSeconds, String bloomKey) {
        try {
            List<CommentLikeDO> commentLikeDOS = commentLikeDOMapper.selectByUserId(userId);
            if (CollUtil.isNotEmpty(commentLikeDOS)) {
                DefaultRedisScript<Long> script = new DefaultRedisScript<>();
                script.setResultType(Long.class);
                script.setScriptSource(new ResourceScriptSource(new ClassPathResource("/lua/bloom_batch_add_comment_like_and_expire.lua")));

                List<Object> luaArgs = Lists.newArrayList();
                commentLikeDOS.forEach(commentLikeDO -> luaArgs.add(commentLikeDO.getCommentId()));
                luaArgs.add(expireSeconds);

                redisTemplate.execute(script, Collections.singletonList(bloomKey), luaArgs.toArray());
            }
        } catch (Exception e) {
            log.error("## 异步初始化【评论点赞】布隆过滤器异常: ", e);
        }
    }

    /**
     * 检查评论是否存在
     *
     * @param commentId
     */
    private void checkCommentIsExist(Long commentId) {
        // 1. 先查本地缓存
        String localCacheJson = LOCAL_CACHE.getIfPresent(commentId);
        if (StringUtils.isBlank(localCacheJson)) {
            // 2. 本地缓存中没有, 走 redis
            String key = RedisKeyConstants.buildCommentDetailKey(commentId);
            Boolean hasKey = redisTemplate.hasKey(key);
            if (!hasKey) {
                // 3. redis 中没有, 走 DB
                CommentDO commentDO = commentDOMapper.selectByPrimaryKey(commentId);
                if (Objects.isNull(commentDO)) {
                    throw new BizException(ResponseCodeEnum.COMMENT_NOT_FOUND);
                }
            }
        }
    }

    /**
     * 设置子评论 VO 计数
     *
     * @param result
     * @param expiredChildCommentIds
     */
    private void setChildCommentCountData(List<FindChildCommentPageListRspVO> result,
                                          List<Long> expiredChildCommentIds) {
        // 准备从评论 Hash 中查询计数 (被点赞数)
        // 缓存中存在的子评论 ID
        List<Long> notExpiredCommentIds = Lists.newArrayList();

        // 提取二级评论 ID
        result.forEach(vo -> {
            Long childCommentId = vo.getCommentId();
            notExpiredCommentIds.add(childCommentId);
        });

        // 从 redis 中查询评论计数 Hash 数据
        Map<Long, Map<Object, Object>> commentIdAndCountMap = getCommentCountDataAndSync2RedisHash(notExpiredCommentIds);

        for (FindChildCommentPageListRspVO rspVO : result) {
            Long commentId = rspVO.getCommentId();
            // 若当前这条评论是从数据库中查询出来的, 则无需设置点赞数, 以数据库查询出来的为主
            if (CollUtil.isNotEmpty(expiredChildCommentIds)
                    && expiredChildCommentIds.contains(commentId)) {
                continue;
            }

            Map<Object, Object> hash = commentIdAndCountMap.get(commentId);
            if (CollUtil.isNotEmpty(hash)) {
                Long likeTotal = Long.valueOf(hash.get(RedisKeyConstants.FIELD_LIKE_TOTAL).toString());
                rspVO.setLikeTotal(likeTotal);
            }
        }
    }

    /**
     * 获取子评论计数数据并同步到 Redis 中
     *
     * @param notExpiredCommentIds
     * @return
     */
    private Map<Long, Map<Object, Object>> getCommentCountDataAndSync2RedisHash(List<Long> notExpiredCommentIds) {
        // 已失效的 Hash 评论 ID
        List<Long> expiredCountCommentIds = Lists.newArrayList();

        List<String> hasKeys = notExpiredCommentIds.stream()
                .map(RedisKeyConstants::buildCountCommentKey)
                .toList();
        List<Object> result = redisTemplate.executePipelined(new SessionCallback<>() {
            @Override
            public Object execute(RedisOperations operations) {
                hasKeys.forEach(key -> operations.opsForHash().entries(key));

                return null;
            }
        });

        Map<Long, Map<Object, Object>> commentIdAndCountMap = Maps.newHashMap();
        for (int i = 0; i < notExpiredCommentIds.size(); i++) {
            Long currCommentId = Long.valueOf(notExpiredCommentIds.get(i).toString());
            Map<Object, Object> hash = (Map<Object, Object>) result.get(i);
            if (CollUtil.isEmpty(hash)) {
                expiredCountCommentIds.add(currCommentId);
                continue;
            }

            commentIdAndCountMap.put(currCommentId, hash);
        }

        // 若已过期的计数评论 ID 集合大于 0, 说明部分计数数据不在 Redis 缓存中
        // 需要查询数据库, 并将这部分的评论计数 Hash 同步到 Redis 中
        if (CollUtil.size(expiredCountCommentIds) > 0) {
            List<CommentDO> commentDOS = commentDOMapper.selectCommentCountByIds(expiredCountCommentIds);
            commentDOS.forEach(commentDO -> {
                Integer level = commentDO.getLevel();
                Map<Object, Object> map = new HashMap<>();
                map.put(RedisKeyConstants.FIELD_LIKE_TOTAL, commentDO.getLikeTotal());

                // 只有一级评论需要统计子评论数
                if (Objects.equals(level, CommentLevelEnum.ONE.getCode())) {
                    map.put(RedisKeyConstants.FIELD_CHILD_COMMENT_TOTAL, commentDO.getChildCommentTotal());
                }

                commentIdAndCountMap.put(commentDO.getId(), map);
            });

            // 异步刷到 redis
            threadPoolTaskExecutor.execute(() -> redisTemplate.executePipelined(new SessionCallback<>() {
                @Override
                public Object execute(RedisOperations operations) {
                    commentDOS.forEach(commentDO -> {
                        String key = RedisKeyConstants.buildCountCommentKey(commentDO.getId());
                        Integer level = commentDO.getLevel();
                        Map<String, Long> fieldsMap = Objects.equals(level, CommentLevelEnum.ONE.getCode())
                                ? Map.of(RedisKeyConstants.FIELD_CHILD_COMMENT_TOTAL, commentDO.getChildCommentTotal(), RedisKeyConstants.FIELD_LIKE_TOTAL, commentDO.getLikeTotal())
                                : Map.of(RedisKeyConstants.FIELD_LIKE_TOTAL, commentDO.getLikeTotal());

                        operations.opsForHash().putAll(key, fieldsMap);

                        long expireTime = 60 * 60 + RandomUtil.randomInt(4 * 60 * 60);
                        operations.expire(key, expireTime, TimeUnit.SECONDS);
                    });
                    return null;
                }
            }));
        }

        return commentIdAndCountMap;
    }

    /**
     * 获取子评论数据并同步到 Redis 中
     *
     * @param childCommentDOS
     * @param result
     */
    private void getChildCommentDataAndSync2Redis(List<CommentDO> childCommentDOS, List<FindChildCommentPageListRspVO> result) {
        // 走 RPC
        List<FindCommentContentReqDTO> findCommentContentReqDTOS = Lists.newArrayList();
        // 调用用户服务的入参
        Set<Long> userIds = Sets.newHashSet();
        Long noteId = null;
        for (CommentDO childCommentDO : childCommentDOS) {
            noteId = childCommentDO.getNoteId();
            Boolean isContentEmpty = childCommentDO.getIsContentEmpty();
            if (!isContentEmpty) {
                FindCommentContentReqDTO reqDTO = FindCommentContentReqDTO.builder()
                        .contentId(childCommentDO.getContentUuid())
                        .yearMonth(DateUtils.formatRelativeTime(childCommentDO.getCreateTime()))
                        .build();
                findCommentContentReqDTOS.add(reqDTO);
            }

            userIds.add(childCommentDO.getUserId());

            Long parentId = childCommentDO.getParentId();
            Long replyCommentId = childCommentDO.getReplyCommentId();
            // 若当前评论的 replyCommentId 不等于 parentId, 则前端需要展示回复的哪个用户, 如  “回复 小林: ”
            if (!Objects.equals(parentId, replyCommentId)) {
                userIds.add(childCommentDO.getReplyUserId());
            }
        }

        // kv-rpc
        List<FindCommentContentRspDTO> findCommentContentRspDTOS = keyValueRpcService.batchFindCommentContent(noteId, findCommentContentReqDTOS);

        Map<String, String> commentUuidAndContentMap = Maps.newHashMap();
        if (CollUtil.isNotEmpty(findCommentContentRspDTOS)) {
            commentUuidAndContentMap = findCommentContentRspDTOS.stream()
                    .collect(Collectors.toMap(FindCommentContentRspDTO::getContentId, FindCommentContentRspDTO::getContent));
        }

        // user-rpc
        List<FindUserByIdRspDTO> findUserByIdRspDTOS = userRpcService.findByIds(userIds.stream().toList());
        Map<Long, FindUserByIdRspDTO> userIdAndUserMap = Maps.newHashMap();
        if (CollUtil.isNotEmpty(findUserByIdRspDTOS)) {
            userIdAndUserMap = findUserByIdRspDTOS.stream()
                    .collect(Collectors.toMap(FindUserByIdRspDTO::getId, dto -> dto));
        }

        for (CommentDO childCommentDO : childCommentDOS) {
            Long userId = childCommentDO.getUserId();
            FindChildCommentPageListRspVO rspVO = FindChildCommentPageListRspVO.builder()
                    .userId(userId)
                    .commentId(childCommentDO.getId())
                    .imageUrl(childCommentDO.getImageUrl())
                    .likeTotal(childCommentDO.getLikeTotal())
                    .createTime(DateUtils.formatRelativeTime(childCommentDO.getCreateTime()))
                    .build();

            // 填充用户信息(包括评论发布者、回复的用户)
            if (CollUtil.isNotEmpty(userIdAndUserMap)) {
                FindUserByIdRspDTO findUserByIdRspDTO = userIdAndUserMap.get(userId);
                // 评论发布者用户信息(头像、昵称)
                if (findUserByIdRspDTO != null) {
                    rspVO.setAvatar(findUserByIdRspDTO.getAvatar());
                    rspVO.setNickname(findUserByIdRspDTO.getNickName());
                }

                // 评论回复的哪个
                Long replyCommentId = childCommentDO.getReplyCommentId();
                Long parentId = childCommentDO.getParentId();
                if (replyCommentId != null && !Objects.equals(parentId, replyCommentId)) {
                    Long replyUserId = childCommentDO.getReplyUserId();
                    FindUserByIdRspDTO replyUserByIdRspDTO = userIdAndUserMap.get(replyUserId);
                    if (replyUserByIdRspDTO != null) {
                        rspVO.setReplyUserName(replyUserByIdRspDTO.getNickName());
                        rspVO.setReplyUserId(replyUserId);
                    }
                }
            }

            if (CollUtil.isNotEmpty(commentUuidAndContentMap)) {
                String contentUuid = childCommentDO.getContentUuid();
                if (StringUtils.isNotBlank(contentUuid)) {
                    String content = commentUuidAndContentMap.get(contentUuid);
                    rspVO.setContent(content);
                }
            }

            result.add(rspVO);
        }

        // 异步刷到 redis
        threadPoolTaskExecutor.execute(() -> {
            Map<String, String> data = Maps.newHashMap();
            result.forEach(vo -> {
                Long commentId = vo.getCommentId();
                String key = RedisKeyConstants.buildCommentDetailKey(commentId);
                data.put(key, JsonUtils.toJsonString(vo));
            });

            batchAddCommentDetailJson2Redis(data);
        });
    }

    /**
     * 批量添加评论详情数据到 Redis 中
     *
     * @param data
     */
    private void batchAddCommentDetailJson2Redis(Map<String, String> data) {
        redisTemplate.executePipelined((RedisCallback<?>) connection -> {
            for (Map.Entry<String, String> entry : data.entrySet()) {
                int randomExpire = 60 * 60 + RandomUtil.randomInt(60 * 60 * 4);

                connection.setEx(redisTemplate.getStringSerializer().serialize(entry.getKey()),
                        randomExpire,
                        redisTemplate.getStringSerializer().serialize(entry.getValue())
                );
            }
            return null;
        });
    }

    /**
     * 同步子评论到 Redis 中
     *
     * @param parentCommentId
     * @param zSetKey
     */
    private void syncChildComment2Redis(Long parentCommentId, String zSetKey) {
        List<CommentDO> childCommentDOS = commentDOMapper.selectChildCommentsByParentIdAndLimit(parentCommentId, 6 * 10);
        if (CollUtil.isNotEmpty(childCommentDOS)) {
            redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
                ZSetOperations<String, Object> opsForZSet = redisTemplate.opsForZSet();

                for (CommentDO childCommentDO : childCommentDOS) {
                    Long commentId = childCommentDO.getId();
                    long commentTimestamp = DateUtils.localDateTime2Timestamp(childCommentDO.getCreateTime());
                    opsForZSet.add(zSetKey, commentId, commentTimestamp);
                }

                int expireTime = 60 * 60 + RandomUtil.randomInt(60 * 60 * 4);
                redisTemplate.expire(zSetKey, expireTime, TimeUnit.SECONDS);
                return null;
            });
        }
    }

    /**
     * 同步评论计数到 Redis 中
     *
     * @param hashKey
     * @param dbCount
     */
    private void syncCommentCount2Redis(String hashKey, Long dbCount) {
        redisTemplate.executePipelined(new SessionCallback<>() {
            @Override
            public Object execute(RedisOperations operations) {
                operations.opsForHash()
                        .put(hashKey, RedisKeyConstants.FIELD_CHILD_COMMENT_TOTAL, dbCount);

                long expireTime = 60 * 60 + RandomUtil.randomInt(60 * 60 * 4);
                operations.expire(hashKey, expireTime, TimeUnit.SECONDS);
                return null;
            }
        });
    }

    /**
     * 设置评论 VO 的计数（从 redis 查询计数）
     *
     * @param result
     * @param expiredCommentIds
     */
    private void setCommentCountData(List<FindCommentItemRspVO> result, List<Long> expiredCommentIds) {
        // 缓存中存在的评论 ID
        List<Long> notExpiredCommentIds = Lists.newArrayList();
        // 提取一级、二级评论 ID
        result.forEach(vo -> {
            Long commentId = vo.getCommentId();
            notExpiredCommentIds.add(commentId);
            FindCommentItemRspVO firstReplyComment = vo.getFirstReplyComment();
            if (Objects.nonNull(firstReplyComment)) {
                notExpiredCommentIds.add(firstReplyComment.getCommentId());
            }
        });

        // 已失效的评论 ID
        Map<Long, Map<Object, Object>> commentIdAndCountMap = getCommentCountDataAndSync2RedisHash(expiredCommentIds);
        for (FindCommentItemRspVO vo : result) {
            Long commentId = vo.getCommentId();
            if (CollUtil.isNotEmpty(expiredCommentIds)
                    && expiredCommentIds.contains(commentId)) {
                continue;
            }

            // 设置一级评论的子评论数、点赞数
            Map<Object, Object> hash = commentIdAndCountMap.get(commentId);
            if (CollUtil.isNotEmpty(hash)) {
                Object childCommentTotalObj = hash.get(RedisKeyConstants.FIELD_CHILD_COMMENT_TOTAL);
                long childCommentTotal = Objects.isNull(childCommentTotalObj) ? 0 : Long.parseLong(childCommentTotalObj.toString());
                Object likeCountObj = hash.get(RedisKeyConstants.FIELD_LIKE_TOTAL);
                long likeTotal = Objects.isNull(likeCountObj) ? 0 : Long.parseLong(likeCountObj.toString());
                vo.setChildCommentTotal(childCommentTotal);
                vo.setLikeTotal(likeTotal);
                // 最早回复的二级评论
                FindCommentItemRspVO firstReplyComment = vo.getFirstReplyComment();
                if (Objects.nonNull(firstReplyComment)) {
                    // 最早回复的二级评论 ID
                    Long firstCommentId = firstReplyComment.getCommentId();
                    Map<Object, Object> firstCommentHash = commentIdAndCountMap.get(firstCommentId);
                    if (CollUtil.isNotEmpty(firstCommentHash)) {
                        Object firstCommentLikeCountObj = firstCommentHash.get(RedisKeyConstants.FIELD_LIKE_TOTAL);
                        long firstCommentLikeTotal = Objects.isNull(firstCommentLikeCountObj) ? 0 : Long.parseLong(firstCommentLikeCountObj.toString());
                        // 设置最早回复的二级评论的点赞数
                        firstReplyComment.setLikeTotal(firstCommentLikeTotal);
                    }
                }
            }
        }
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

                batchAddCommentDetailJson2Redis(data);
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
