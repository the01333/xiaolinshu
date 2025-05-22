package com.puxinxiaolin.xiaolinshu.auth.service.impl;

import cn.dev33.satoken.stp.SaTokenInfo;
import cn.dev33.satoken.stp.StpUtil;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.puxinxiaolin.framework.common.enums.DeletedEnum;
import com.puxinxiaolin.framework.common.enums.StatusEnum;
import com.puxinxiaolin.framework.common.exception.BizException;
import com.puxinxiaolin.framework.common.response.Response;
import com.puxinxiaolin.framework.common.util.JsonUtils;
import com.puxinxiaolin.xiaolinshu.auth.constant.RedisKeyConstants;
import com.puxinxiaolin.xiaolinshu.auth.constant.RoleConstants;
import com.puxinxiaolin.xiaolinshu.auth.domain.dataobject.RoleDO;
import com.puxinxiaolin.xiaolinshu.auth.domain.dataobject.UserDO;
import com.puxinxiaolin.xiaolinshu.auth.domain.dataobject.UserRoleDO;
import com.puxinxiaolin.xiaolinshu.auth.domain.mapper.RoleDOMapper;
import com.puxinxiaolin.xiaolinshu.auth.domain.mapper.UserDOMapper;
import com.puxinxiaolin.xiaolinshu.auth.domain.mapper.UserRoleDOMapper;
import com.puxinxiaolin.xiaolinshu.auth.enums.LoginTypeEnum;
import com.puxinxiaolin.xiaolinshu.auth.enums.ResponseCodeEnum;
import com.puxinxiaolin.xiaolinshu.auth.model.vo.user.UserLoginReqVO;
import com.puxinxiaolin.xiaolinshu.auth.service.UserService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Slf4j
@Service
public class UserServiceImpl implements UserService {
    @Resource
    private UserDOMapper userDOMapper;
    @Resource
    private UserRoleDOMapper userRoleDOMapper;
    @Resource
    private RoleDOMapper roleDOMapper;
    @Resource
    private RedisTemplate<String, Object> redisTemplate;
    @Resource
    private TransactionTemplate transactionTemplate;

    @Override
    public Response<String> loginAndRegister(UserLoginReqVO userLoginReqVO) {
        String phone = userLoginReqVO.getPhone();
        Integer type = userLoginReqVO.getType();

        LoginTypeEnum loginTypeEnum = LoginTypeEnum.valueOf(type);
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

                // 删除验证码缓存
                redisTemplate.delete(key);
                UserDO userDO = userDOMapper.selectByPhone(phone);
                log.info("==> 用户是否注册, phone: {}, userDO: {}", phone, JsonUtils.toJsonString(userDO));

                if (Objects.isNull(userDO)) {
                    // 还未注册就自动注册
                    userId = registerUser(phone);
                } else {
                    userId = userDO.getId();
                }
            }
            case PASSWORD -> {
                // TODO [YCcLin 2025/5/21]: 密码登录
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
     * 自动注册用户
     *
     * @param phone
     * @return
     */
    public Long registerUser(String phone) {
        return transactionTemplate.execute(status -> {
            try {
                Long xiaolinshuId = redisTemplate.opsForValue()
                        .increment(RedisKeyConstants.XIAOLINSHU_ID_GENERATOR_KEY);

                UserDO userDO = UserDO.builder()
                        .phone(phone)
                        .xiaolinshuId(String.valueOf(xiaolinshuId))
                        .nickname("小林薯" + xiaolinshuId)
                        .status(StatusEnum.ENABLE.getValue())
                        .createTime(LocalDateTime.now())
                        .updateTime(LocalDateTime.now())
                        .isDeleted(DeletedEnum.NO.getValue())
                        .build();
                userDOMapper.insert(userDO);

                Long userId = userDO.getId();

                UserRoleDO userRoleDO = UserRoleDO.builder()
                        .userId(userId)
                        .roleId(RoleConstants.COMMON_USER_ROLE_ID)
                        .createTime(LocalDateTime.now())
                        .updateTime(LocalDateTime.now())
                        .isDeleted(DeletedEnum.NO.getValue())
                        .build();
                userRoleDOMapper.insert(userRoleDO);

                RoleDO roleDO = roleDOMapper.selectByPrimaryKey(RoleConstants.COMMON_USER_ROLE_ID);

                // 缓存用户的角色 ID
                List<String> roles = new ArrayList<>(1);
                roles.add(roleDO.getRoleKey());
                
                String userRolesKey = RedisKeyConstants.buildUserRoleKey(userId);
                redisTemplate.opsForValue().set(userRolesKey, JsonUtils.toJsonString(roles));

                return userId;
            } catch (Exception e) {
                status.setRollbackOnly();
                log.error("==> 注册用户失败: {}", e.getMessage(), e);
                return null;
            }
        });
    }

}
