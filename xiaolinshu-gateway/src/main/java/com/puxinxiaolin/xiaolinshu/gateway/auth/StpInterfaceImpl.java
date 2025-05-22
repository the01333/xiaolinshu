package com.puxinxiaolin.xiaolinshu.gateway.auth;

import cn.dev33.satoken.stp.StpInterface;
import cn.hutool.core.collection.CollUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.puxinxiaolin.xiaolinshu.gateway.constant.RedisKeyConstants;
import jakarta.annotation.Resource;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @Description: 自定义权限验证接口扩展
 * @Author: YCcLin
 * @Date: 2025/5/22 14:52
 */
@Slf4j
@Component
public class StpInterfaceImpl implements StpInterface {
    @Resource
    private ObjectMapper objectMapper;
    @Resource
    private RedisTemplate<String, String> redisTemplate;

    @SneakyThrows
    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        log.info("## 获取用户权限列表, loginId: {}", loginId);

        String userRolesKey = RedisKeyConstants.buildUserRoleKey(Long.valueOf(loginId.toString()));
        String userRolesValue = redisTemplate.opsForValue().get(userRolesKey);
        if (StringUtils.isBlank(userRolesValue)) {
            return null;
        }

        List<String> userRoleKeys = objectMapper.readValue(userRolesValue, new TypeReference<>() {
        });
        if (CollUtil.isNotEmpty(userRoleKeys)) {
            // 可能有多个角色，对应多个权限
            List<String> rolePermissionsKeys = userRoleKeys.stream()
                    .map(RedisKeyConstants::buildRolePermissionsKey)
                    .toList();
            List<String> rolePermissionsValues = redisTemplate.opsForValue()
                    .multiGet(rolePermissionsKeys);
            if (CollUtil.isNotEmpty(rolePermissionsValues)) {
                List<String> permissions = Lists.newArrayList();

                rolePermissionsValues.forEach(value -> {
                    try {
                        List<String> rolePermissions = objectMapper.readValue(value, new TypeReference<>() {
                        });
                        permissions.addAll(rolePermissions);
                    } catch (JsonProcessingException ex) {
                        log.error("==> JSON 解析错误: {}", ex.getMessage(), ex);
                    }
                });
                
                return permissions;
            }
        }
        
        return null;
    }

    @SneakyThrows
    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        log.info("## 获取用户角色列表, loginId: {}", loginId);

        String userRolesKey = RedisKeyConstants.buildUserRoleKey(Long.valueOf(loginId.toString()));
        String userRolesValue = redisTemplate.opsForValue().get(userRolesKey);
        if (StringUtils.isBlank(userRolesValue)) {
            return null;
        }

        return objectMapper.readValue(userRolesValue, new TypeReference<>() {
        });
    }

}
