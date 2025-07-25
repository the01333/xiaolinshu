package com.puxinxiaolin.xiaolinshu.user.relation.biz.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.RandomUtil;
import com.puxinxiaolin.framework.biz.context.holder.LoginUserContextHolder;
import com.puxinxiaolin.framework.common.exception.BizException;
import com.puxinxiaolin.framework.common.response.PageResponse;
import com.puxinxiaolin.framework.common.response.Response;
import com.puxinxiaolin.framework.common.util.DateUtils;
import com.puxinxiaolin.framework.common.util.JsonUtils;
import com.puxinxiaolin.xiaolinshu.user.api.dto.resp.FindUserByIdRspDTO;
import com.puxinxiaolin.xiaolinshu.user.relation.biz.constant.MqConstants;
import com.puxinxiaolin.xiaolinshu.user.relation.biz.constant.RedisKeyConstants;
import com.puxinxiaolin.xiaolinshu.user.relation.biz.domain.dataobject.FansDO;
import com.puxinxiaolin.xiaolinshu.user.relation.biz.domain.dataobject.FollowingDO;
import com.puxinxiaolin.xiaolinshu.user.relation.biz.domain.mapper.FansDOMapper;
import com.puxinxiaolin.xiaolinshu.user.relation.biz.domain.mapper.FollowingDOMapper;
import com.puxinxiaolin.xiaolinshu.user.relation.biz.enums.LuaResultEnum;
import com.puxinxiaolin.xiaolinshu.user.relation.biz.enums.ResponseCodeEnum;
import com.puxinxiaolin.xiaolinshu.user.relation.biz.model.dto.FollowUserMqDTO;
import com.puxinxiaolin.xiaolinshu.user.relation.biz.model.dto.UnfollowUserMqDTO;
import com.puxinxiaolin.xiaolinshu.user.relation.biz.model.vo.*;
import com.puxinxiaolin.xiaolinshu.user.relation.biz.rpc.UserRpcService;
import com.puxinxiaolin.xiaolinshu.user.relation.biz.service.RelationService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Service
@Slf4j
public class RelationServiceImpl implements RelationService {
    @Resource
    private FollowingDOMapper followingDOMapper;
    @Resource
    private FansDOMapper fansDOMapper;
    @Resource
    private RedisTemplate<String, Object> redisTemplate;
    @Resource
    private RocketMQTemplate rocketMQTemplate;
    @Resource
    private UserRpcService userRpcService;
    @Resource(name = "taskExecutor")
    private ThreadPoolTaskExecutor threadPoolTaskExecutor;

    /**
     * 关注用户, 这里用 lua 脚本实现原子性
     *
     * @param request
     * @return
     */
    @Override
    public Response<?> follow(FollowUserReqVO request) {
        Long followUserId = request.getFollowUserId();
        Long currentUserId = LoginUserContextHolder.getUserId();
        if (Objects.equals(followUserId, currentUserId)) {
            throw new BizException(ResponseCodeEnum.CANT_FOLLOW_YOUR_SELF);
        }

        // 关注的用户是否存在 
        FindUserByIdRspDTO findUserByIdRspDTO = userRpcService.findById(followUserId);
        if (findUserByIdRspDTO == null) {
            throw new BizException(ResponseCodeEnum.FOLLOW_USER_NOT_EXISTED);
        }
        
        // 关注数是否已达上限
        // 用 lua 脚本校验并添加关注关系
        String key = RedisKeyConstants.buildUserFollowingKey(currentUserId);
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setScriptSource(new ResourceScriptSource(new ClassPathResource("/lua/follow_check_and_add.lua")));
        script.setResultType(Long.class);

        LocalDateTime now = LocalDateTime.now();
        long nowTimestamp = DateUtils.localDateTime2Timestamp(now);

        Long result = redisTemplate.execute(script, Collections.singletonList(key), followUserId, nowTimestamp);

        // 校验 Lua 脚本执行结果
        checkLuaScriptResult(result);

        // 写入 ZSET
        if (Objects.equals(result, LuaResultEnum.ZSET_NOT_EXIST.getCode())) {
            List<FollowingDO> followingDOList = followingDOMapper.selectByUserId(currentUserId);
            // 保底1天 + 随机秒数
            long expireSeconds = 60 * 60 * 24 + RandomUtil.randomInt(60 * 60 * 24);

            // 若 DB 记录为空, 直接把当前这次关注的关系数据加入 ZSET 中
            if (CollUtil.isEmpty(followingDOList)) {
                DefaultRedisScript<Long> script2 = new DefaultRedisScript<>();
                script2.setScriptSource(new ResourceScriptSource(new ClassPathResource("/lua/follow_add_and_expire.lua")));
                script2.setResultType(Long.class);

                // TODO [YCcLin 2025/6/4]: 可以根据用户类型, 设置不同的过期时间 ->
                //  若当前用户为大V, 则可以过期时间设置的长些或者不设置过期时间; 如不是, 则设置的短些
                // 可以从计数服务获取用户的粉丝数, 目前计数服务还没创建, 则暂时采用统一的过期策略
                redisTemplate.execute(script2, Collections.singletonList(key), followUserId, nowTimestamp, expireSeconds);
            } else {
                // 刷新 DB 记录到 ZSET 中
                Object[] luaArgs = buildLuaArgs(followingDOList, expireSeconds);

                DefaultRedisScript<Long> script3 = new DefaultRedisScript<>();
                script3.setScriptSource(new ResourceScriptSource(new ClassPathResource("/lua/follow_batch_add_and_expire.lua")));
                script3.setResultType(Long.class);
                redisTemplate.execute(script3, Collections.singletonList(key), luaArgs);

                // 还需要把当前这次的关注关系数据加入 ZSET 中
                result = redisTemplate.execute(script, Collections.singletonList(key), followUserId, nowTimestamp);
                checkLuaScriptResult(result);
            }
        }

        // 走 MQ
        FollowUserMqDTO mqDTO = FollowUserMqDTO.builder()
                .userId(currentUserId)
                .followUserId(followUserId)
                .createTime(now)
                .build();
        Message<String> message = MessageBuilder.withPayload(JsonUtils.toJsonString(mqDTO))
                .build();
        String destination = MqConstants.TOPIC_FOLLOW_OR_UNFOLLOW + ":" + MqConstants.TAG_FOLLOW;

        log.info("==> 开始发送关注操作 MQ, 消息体: {}", mqDTO);

        String hashKey = String.valueOf(currentUserId);
        rocketMQTemplate.asyncSendOrderly(destination, message, hashKey, new SendCallback() {
            @Override
            public void onSuccess(SendResult sendResult) {
                log.info("==> MQ 发送成功，SendResult: {}", sendResult);
            }

            @Override
            public void onException(Throwable ex) {
                log.error("==> MQ 发送异常: ", ex);
            }
        });

        return Response.success();
    }

    /**
     * 取关用户
     *
     * @param request
     * @return
     */
    @Override
    public Response<?> unfollow(UnfollowUserReqVO request) {
        Long unfollowUserId = request.getUnfollowUserId();
        Long currentUserId = LoginUserContextHolder.getUserId();
        if (Objects.equals(unfollowUserId, currentUserId)) {
            throw new BizException(ResponseCodeEnum.CANT_UNFOLLOW_YOUR_SELF);
        }

        // 取关的用户是否存在
        FindUserByIdRspDTO findUserByIdRspDTO = userRpcService.findById(unfollowUserId);
        if (findUserByIdRspDTO == null) {
            throw new BizException(ResponseCodeEnum.FOLLOW_USER_NOT_EXISTED);
        }

        // 必须关注才能取关
        String key = RedisKeyConstants.buildUserFollowingKey(currentUserId);

        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setScriptSource(new ResourceScriptSource(new ClassPathResource("/lua/unfollow_check_and_delete.lua")));
        script.setResultType(Long.class);

        Long result = redisTemplate.execute(script, Collections.singletonList(key), unfollowUserId);
        if (Objects.equals(result, LuaResultEnum.NOT_FOLLOWED.getCode())) {
            throw new BizException(ResponseCodeEnum.NOT_FOLLOWED);
        }
        if (Objects.equals(result, LuaResultEnum.ZSET_NOT_EXIST.getCode())) {
            // 批量同步
            int expireSeconds = 60 * 60 * 24 + RandomUtil.randomInt(60 * 60 * 24);

            List<FollowingDO> followingDOList = followingDOMapper.selectByUserId(currentUserId);
            if (CollUtil.isEmpty(followingDOList)) {
                throw new BizException(ResponseCodeEnum.NOT_FOLLOWED);
            } else {
                Object[] luaArgs = buildLuaArgs(followingDOList, expireSeconds);

                DefaultRedisScript<Long> script2 = new DefaultRedisScript<>();
                script2.setScriptSource(new ResourceScriptSource(new ClassPathResource("/lua/follow_batch_add_and_expire.lua")));
                script2.setResultType(Long.class);

                redisTemplate.execute(script2, Collections.singletonList(key), luaArgs);

                // 清除旧数据: 把要取关的用户关系数据删除
                result = redisTemplate.execute(script, Collections.singletonList(key), unfollowUserId);
                if (Objects.equals(result, LuaResultEnum.NOT_FOLLOWED.getCode())) {
                    throw new BizException(ResponseCodeEnum.NOT_FOLLOWED);
                }
            }
        }

        // 走 MQ
        UnfollowUserMqDTO mqDTO = UnfollowUserMqDTO.builder()
                .userId(currentUserId)
                .unfollowUserId(unfollowUserId)
                .createTime(LocalDateTime.now())
                .build();
        Message<String> message = MessageBuilder.withPayload(JsonUtils.toJsonString(mqDTO))
                .build();
        String destination = MqConstants.TOPIC_FOLLOW_OR_UNFOLLOW + ":" + MqConstants.TAG_UNFOLLOW;

        log.info("==> 开始发送取关操作 MQ, 消息体: {}", mqDTO);

        String hashKey = String.valueOf(currentUserId);
        rocketMQTemplate.asyncSendOrderly(destination, message, hashKey, new SendCallback() {
            @Override
            public void onSuccess(SendResult sendResult) {
                log.info("==> MQ 发送成功, SendResult: {}", sendResult);
            }

            @Override
            public void onException(Throwable ex) {
                log.error("==> MQ 发送异常: ", ex);
            }
        });

        return Response.success();
    }

    /**
     * 查询用户关注列表
     *
     * @param request
     * @return
     */
    @Override
    public PageResponse<FindFollowingUserRspVO> findFollowingList(FindFollowingListReqVO request) {
        List<FindFollowingUserRspVO> result = null;

        Long userId = request.getUserId();
        Integer pageNo = request.getPageNo();
        long limit = 10;

        String key = RedisKeyConstants.buildUserFollowingKey(userId);
        // 关注的数量
        Long total = redisTemplate.opsForZSet().zCard(key);
        if (total > 0) {
            long totalPage = PageResponse.getTotalPage(total, limit);

            if (pageNo > totalPage) {
                return PageResponse.success(null, pageNo, total);
            }

            // 偏移量
            long offset = PageResponse.getOffset(pageNo, limit);
            /*
                使用 ZREVRANGEBYSCORE 命令按 score 降序获取元素, 同时使用 LIMIT 子句实现分页
                注意: 这里使用了 Double.POSITIVE_INFINITY 和 Double.NEGATIVE_INFINITY 作为分数范围
                因为关注列表最多有 1000 个元素, 这样可以确保获取到所有的元素
             */
            Set<Object> followingUserIdsSet = redisTemplate.opsForZSet()
                    .reverseRangeByScore(key, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, offset, limit);
            if (CollUtil.isNotEmpty(followingUserIdsSet)) {
                List<Long> userIdList = followingUserIdsSet.stream()
                        .map(id -> Long.valueOf(id.toString()))
                        .toList();

                // 走 RPC
                List<FindUserByIdRspDTO> userDtoList = userRpcService.findByIds(userIdList);
                if (CollUtil.isNotEmpty(userDtoList)) {
                    result = convertDTOs2VOs(userDtoList);
                }
            }
        } else {
            // 走 DB
            long count = followingDOMapper.selectCountByUserId(userId);
            long totalPage2 = PageResponse.getTotalPage(count, limit);
            if (pageNo > totalPage2) {
                return PageResponse.success(null, pageNo, count);
            }

            long offset2 = PageResponse.getOffset(pageNo, limit);
            List<FollowingDO> followingDOList = followingDOMapper.selectPageListByUserId(userId, offset2, limit);
            total = count;
            if (CollUtil.isNotEmpty(followingDOList)) {
                List<Long> userIdList = followingDOList.stream()
                        .map(FollowingDO::getId)
                        .toList();

                // 走 RPC
                List<FindUserByIdRspDTO> resultDto = userRpcService.findByIds(userIdList);
                if (CollUtil.isNotEmpty(resultDto)) {
                    result = convertDTOs2VOs(resultDto);
                }

                // 把 DB 的数据刷到 redis
                threadPoolTaskExecutor.submit(() ->
                        syncFollowingList2Redis(userId)
                );
            }
        }

        return PageResponse.success(result, pageNo, total);
    }

    /**
     * 查询用户粉丝列表
     *
     * @param request
     * @return
     */
    @Override
    public PageResponse<FindFansUserRspVO> findFansList(FindFansListReqVO request) {
        List<FindFansUserRspVO> result = null;

        Long userId = request.getUserId();
        Integer pageNo = request.getPageNo();
        long limit = 10;

        String key = RedisKeyConstants.buildUserFansKey(userId);
        Long total = redisTemplate.opsForZSet().zCard(key);
        if (total > 0) {
            // 总页数
            long totalPage = PageResponse.getTotalPage(total, limit);
            if (pageNo > totalPage) {
                return PageResponse.success(null, pageNo, total);
            }

            // 偏移量
            long offset = PageResponse.getOffset(pageNo, limit);

            Set<Object> fansUserIdsSet = redisTemplate.opsForZSet()
                    .reverseRangeByScore(key, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, offset, limit);
            if (CollUtil.isNotEmpty(fansUserIdsSet)) {
                List<Long> userIdList = fansUserIdsSet.stream()
                        .map(id -> Long.valueOf(id.toString()))
                        .toList();

                // RPC: 调用用户服务、计数服务, 并将 DTO 转换为 VO
                result = rpcUserServiceAndCountServiceAndConvertDTOs2VOs(userIdList, result);
            }
        } else {
            total = fansDOMapper.selectCountByUserId(userId);
            long totalPage = PageResponse.getTotalPage(total, limit);
            if (pageNo > 500 || pageNo > totalPage) {
                return PageResponse.success(null, pageNo, total);
            }

            long offset = PageResponse.getOffset(pageNo, limit);

            List<FansDO> fansDOS = fansDOMapper.selectPageListByUserId(userId, offset, limit);
            if (CollUtil.isNotEmpty(fansDOS)) {
                List<Long> userIdList = fansDOS.stream()
                        .map(FansDO::getFansUserId)
                        .toList();

                // RPC: 调用用户服务、计数服务, 并将 DTO 转换为 VO
                result = rpcUserServiceAndCountServiceAndConvertDTOs2VOs(userIdList, result);

                // 异步将粉丝列表同步到 Redis（最多5000条）
                threadPoolTaskExecutor.submit(() -> syncFansList2Redis(userId));
            }
        }

        return PageResponse.success(result, pageNo, total);
    }

    /**
     * 走RPC: 调用用户服务、计数服务, 并将 DTO 转换为 VO
     *
     * @param userIdList
     * @param result
     * @return
     */
    private List<FindFansUserRspVO> rpcUserServiceAndCountServiceAndConvertDTOs2VOs(List<Long> userIdList,
                                                                                    List<FindFansUserRspVO> result) {
        List<FindUserByIdRspDTO> userDtoList = userRpcService.findByIds(userIdList);

        // TODO [YCcLin 2025/6/7]: 批量查询用户的计数数据（笔记总数、粉丝总数）

        if (CollUtil.isNotEmpty(userDtoList)) {
            result = userDtoList.stream()
                    .map(dto -> FindFansUserRspVO.builder()
                            .userId(dto.getId())
                            .avatar(dto.getAvatar())
                            .nickname(dto.getNickName())
                            .noteTotal(0L)  // TODO: 这块的数据暂无, 后续补充
                            .fansTotal(0L)  // TODO: 这块的数据暂无, 后续补充
                            .build())
                    .toList();
        }
        return result;
    }

    /**
     * 批量同步粉丝列表到 Redis
     *
     * @param userId
     */
    private void syncFansList2Redis(Long userId) {
        List<FansDO> fansDOS = fansDOMapper.select5000FansByUserId(userId);
        if (CollUtil.isNotEmpty(fansDOS)) {
            String key = RedisKeyConstants.buildUserFansKey(userId);
            int expireSeconds = 60 * 60 * 24 + RandomUtil.randomInt(60 * 60 * 24);

            Object[] luaArgs = buildFansZSetLuaArgs(fansDOS, expireSeconds);

            DefaultRedisScript<Long> script = new DefaultRedisScript<>();
            script.setScriptSource(new ResourceScriptSource(new ClassPathResource("/lua/follow_batch_add_and_expire.lua")));
            script.setResultType(Long.class);
            
            redisTemplate.execute(script, Collections.singletonList(key), luaArgs);
        }
    }

    /**
     * 构建粉丝列表的 Lua 脚本的参数
     *
     * @param fansDOS
     * @param expireSeconds
     * @return
     */
    private Object[] buildFansZSetLuaArgs(List<FansDO> fansDOS, int expireSeconds) {
        int argsLength = fansDOS.size() * 2 + 1;
        Object[] luaArgs = new Object[argsLength];
        
        int i = 0;
        for (FansDO fansDO : fansDOS) {
            luaArgs[i] = DateUtils.localDateTime2Timestamp(fansDO.getCreateTime());
            luaArgs[i + 1] = fansDO.getFansUserId();
            i += 2;
        }
        
        luaArgs[argsLength - 1] = expireSeconds;
        return luaArgs;
    }

    /**
     * 批量同步关注列表到 Redis
     *
     * @param userId
     */
    private void syncFollowingList2Redis(Long userId) {
        List<FollowingDO> followingDOList = followingDOMapper.selectAllByUserId(userId);

        if (CollUtil.isNotEmpty(followingDOList)) {
            String key = RedisKeyConstants.buildUserFollowingKey(userId);
            int expireSeconds = 60 * 60 * 24 + RandomUtil.randomInt(60 * 60 * 24);
            Object[] luaArgs = buildLuaArgs(followingDOList, expireSeconds);

            DefaultRedisScript<Long> script = new DefaultRedisScript<>();
            script.setScriptSource(new ResourceScriptSource(new ClassPathResource("/lua/follow_batch_add_and_expire.lua")));
            script.setResultType(Long.class);

            redisTemplate.execute(script, Collections.singletonList(key), luaArgs);
        }
    }

    /**
     * List<FindUserByIdRspDTO> -> List<FindFollowingUserRspVO>
     *
     * @param dtoList
     * @return
     */
    private List<FindFollowingUserRspVO> convertDTOs2VOs(List<FindUserByIdRspDTO> dtoList) {
        return dtoList.stream()
                .map(dto -> FindFollowingUserRspVO.builder()
                        .nickname(dto.getNickName())
                        .avatar(dto.getAvatar())
                        .introduction(dto.getIntroduction())
                        .userId(dto.getId())
                        .build()
                ).toList();
    }

    /**
     * 构建关注列表的 lua 脚本参数 -> {关注时间/score, 关注的用户ID/value, ..., 过期时间}
     *
     * @param followingDOList
     * @param expireSeconds
     * @return
     */
    private Object[] buildLuaArgs(List<FollowingDO> followingDOList, long expireSeconds) {
        // 每个关注关系有两个参数（score 和 value）, 还有一个过期时间
        int argsLength = followingDOList.size() * 2 + 1;
        Object[] luaArgs = new Object[argsLength];

        int i = 0;
        for (FollowingDO followingDO : followingDOList) {
            luaArgs[i] = DateUtils.localDateTime2Timestamp(followingDO.getCreateTime());
            luaArgs[i + 1] = followingDO.getFollowingUserId();
            i += 2;
        }

        luaArgs[argsLength - 1] = expireSeconds;
        return luaArgs;
    }

    /**
     * 校验 Lua 脚本执行结果, 根据状态码抛出对应的业务异常
     *
     * @param result
     */
    private void checkLuaScriptResult(Long result) {
        LuaResultEnum luaResultEnum = LuaResultEnum.valueOf(result);
        if (luaResultEnum == null) {
            throw new RuntimeException("Lua 返回结果错误");
        }

        switch (luaResultEnum) {
            case FOLLOW_LIMIT -> throw new BizException(ResponseCodeEnum.FOLLOWING_COUNT_LIMIT);
            case ALREADY_FOLLOWED -> throw new BizException(ResponseCodeEnum.ALREADY_FOLLOWED);
        }
    }

}
