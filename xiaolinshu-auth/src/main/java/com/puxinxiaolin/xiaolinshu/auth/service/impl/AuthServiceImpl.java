package com.puxinxiaolin.xiaolinshu.auth.service.impl;

import cn.dev33.satoken.stp.SaTokenInfo;
import cn.dev33.satoken.stp.StpUtil;
import com.google.common.base.Preconditions;
import com.puxinxiaolin.framework.biz.context.holder.LoginUserContextHolder;
import com.puxinxiaolin.framework.common.exception.BizException;
import com.puxinxiaolin.framework.common.response.Response;
import com.puxinxiaolin.xiaolinshu.auth.constant.RedisKeyConstants;
import com.puxinxiaolin.xiaolinshu.auth.enums.LoginTypeEnum;
import com.puxinxiaolin.xiaolinshu.auth.enums.ResponseCodeEnum;
import com.puxinxiaolin.xiaolinshu.auth.model.vo.user.UpdatePasswordReqVO;
import com.puxinxiaolin.xiaolinshu.auth.model.vo.user.UserLoginReqVO;
import com.puxinxiaolin.xiaolinshu.auth.rpc.UserRpcService;
import com.puxinxiaolin.xiaolinshu.auth.service.AuthService;
import com.puxinxiaolin.xiaolinshu.user.api.dto.resp.FindUserByPhoneRspDTO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Slf4j
@Service
public class AuthServiceImpl implements AuthService {
    @Resource
    private RedisTemplate<String, Object> redisTemplate;
    @Resource(name = "taskExecutor")
    private ThreadPoolTaskExecutor threadPoolTaskExecutor;
    @Resource
    private PasswordEncoder passwordEncoder;
    @Resource
    private UserRpcService userRpcService;

    /**
     * 修改密码
     *
     * @param request
     * @return
     */
    @Override
    public Response<?> updatePassword(UpdatePasswordReqVO request) {
        String newPassword = request.getNewPassword();
        String encodePassword = passwordEncoder.encode(newPassword);

        // 走 rpc
        userRpcService.updatePassword(encodePassword);

        return Response.success();
    }

    /**
     * 登录与注册
     *
     * @param userLoginReqVO
     * @return
     */
    @Override
    public Response<String> loginAndRegister(UserLoginReqVO userLoginReqVO) {
        String phone = userLoginReqVO.getPhone();
        Integer type = userLoginReqVO.getType();

        LoginTypeEnum loginTypeEnum = LoginTypeEnum.valueOf(type);
        if (Objects.isNull(loginTypeEnum)) {
            throw new BizException(ResponseCodeEnum.LOGIN_TYPE_ERROR);
        }

        Long userId = null;

        switch (loginTypeEnum) {
            case VERIFICATION_CODE -> {
                String verificationCode = userLoginReqVO.getCode();

                Preconditions.checkArgument(StringUtils.isNotBlank(verificationCode), "验证码不能为空");

                String key = RedisKeyConstants.buildVerificationCodeKey(phone);
                String senCode = (String) redisTemplate.opsForValue().get(key);
                if (!StringUtils.equals(senCode, verificationCode)) {
                    throw new BizException(ResponseCodeEnum.VERIFICATION_CODE_ERROR);
                }

                // 拿到缓存后需要马上删除缓存
                redisTemplate.delete(key);

                // 走 rpc
                Long tempUserId = userRpcService.registerUser(phone);
                if (Objects.isNull(tempUserId)) {
                    throw new BizException(ResponseCodeEnum.LOGIN_FAIL);
                }

                userId = tempUserId;
            }
            case PASSWORD -> {
                String password = userLoginReqVO.getPassword();

                // 走 rpc
                FindUserByPhoneRspDTO existUserDO = userRpcService.findUserByPhone(phone);
                if (Objects.isNull(existUserDO)) {
                    throw new BizException(ResponseCodeEnum.USER_NOT_FOUND);
                }

                String encodePassword = existUserDO.getPassword();
                boolean isMatched = passwordEncoder.matches(password, encodePassword);
                if (!isMatched) {
                    throw new BizException(ResponseCodeEnum.PHONE_OR_PASSWORD_ERROR);
                }

                userId = existUserDO.getId();
            }
            default -> {
            }
        }

        // SaToken 登录用户并返回令牌
        StpUtil.login(userId);

        SaTokenInfo tokenInfo = StpUtil.getTokenInfo();
        return Response.success(tokenInfo.tokenValue);
    }

    /**
     * 退出登录
     *
     * @return
     */
    @Override
    public Response<?> logout() {
        Long userId = LoginUserContextHolder.getUserId();

        log.info("==> 用户退出登录, userId: {}", userId);

        threadPoolTaskExecutor.submit(() -> {
            Long userId2 = LoginUserContextHolder.getUserId();
            log.info("==> 异步线程中获取 userId: {}", userId2);
        });

        StpUtil.logout(userId);
        return Response.success();
    }

}
