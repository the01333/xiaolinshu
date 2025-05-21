package com.puxinxiaolin.xiaolinshu.auth.runner;

import cn.hutool.core.collection.CollUtil;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.puxinxiaolin.framework.common.util.JsonUtils;
import com.puxinxiaolin.xiaolinshu.auth.constant.RedisKeyConstants;
import com.puxinxiaolin.xiaolinshu.auth.domain.dataobject.PermissionDO;
import com.puxinxiaolin.xiaolinshu.auth.domain.dataobject.RoleDO;
import com.puxinxiaolin.xiaolinshu.auth.domain.dataobject.RolePermissionDO;
import com.puxinxiaolin.xiaolinshu.auth.domain.mapper.PermissionDOMapper;
import com.puxinxiaolin.xiaolinshu.auth.domain.mapper.RoleDOMapper;
import com.puxinxiaolin.xiaolinshu.auth.domain.mapper.RolePermissionDOMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Component
public class PushRolePermission2RedisRunner implements ApplicationRunner {
    @Resource
    private RedisTemplate<String, Object> redisTemplate;
    @Resource
    private RoleDOMapper roleDOMapper;
    @Resource
    private PermissionDOMapper permissionDOMapper;
    @Resource
    private RolePermissionDOMapper rolePermissionDOMapper;
    
    // 用于分布式锁的 flag
    public static final String PUSH_PERMISSION_FLAG = "push.permission.flag";

    @Override
    public void run(ApplicationArguments args) throws Exception {
        log.info("==> 服务启动，开始同步角色权限数据到 Redis 中...");

        try {

            Boolean canPushed = redisTemplate.opsForValue()
                    .setIfAbsent(PUSH_PERMISSION_FLAG, "1", 1, TimeUnit.DAYS);
            if (Boolean.FALSE.equals(canPushed)) { 
                log.warn("==> 角色权限数据已经同步至 Redis 中，不再同步...");
                return;
            }

            List<RoleDO> roleDOList = roleDOMapper.selectEnabledList();
            if (!CollectionUtils.isEmpty(roleDOList)) {
                List<Long> roleIds = roleDOList.stream()
                        .map(RoleDO::getId)
                        .toList();

                List<RolePermissionDO> rolePermissionDOList = rolePermissionDOMapper.selectByRoleIds(roleIds);
                Map<Long, List<Long>> roleIdWithPermissionIdsMap = rolePermissionDOList.stream()
                        // Map<roleId, List<permissionId>>
                        .collect(Collectors.groupingBy(RolePermissionDO::getRoleId,
                                Collectors.mapping(RolePermissionDO::getPermissionId, Collectors.toList()))
                        );

                List<PermissionDO> permissionDOList = permissionDOMapper.selectAppEnabledList();
                // Map<permissionId, permissionDO>
                Map<Long, PermissionDO> permissionIdWithDOMap = permissionDOList.stream()
                        .collect(Collectors.toMap(PermissionDO::getId, permissionDO -> permissionDO));
                
                // Map<roleId, List<permissionDO>>
                Map<Long, List<PermissionDO>> roleIdWithPermissionDOMap = Maps.newHashMap();
                roleDOList.forEach(roleDO -> {
                    Long roleId = roleDO.getId();
                    List<Long> permissionIds = roleIdWithPermissionIdsMap.get(roleId);
                    if (CollUtil.isNotEmpty(permissionIds)) {
                        List<PermissionDO> permissionDOS = Lists.newArrayList();
                        permissionIds.forEach(permissionId -> {
                            PermissionDO permissionDO = permissionIdWithDOMap.get(permissionId);
                            if (Objects.nonNull(permissionDO)) {
                                permissionDOS.add(permissionDO);
                            }
                        });
                        
                        roleIdWithPermissionDOMap.put(roleId, permissionDOS);
                    }
                });

                // 同步到 redis
                roleIdWithPermissionDOMap.forEach((roleId, permissionDOS) -> {
                    String key = RedisKeyConstants.buildRolePermissionsKey(roleId);
                    redisTemplate.opsForValue().set(key, JsonUtils.toJsonString(permissionDOS));
                });
            }

            log.info("==> 服务启动，成功同步角色权限数据到 Redis 中...");
        } catch (Exception e) {
            log.error("==> 同步角色权限数据到 Redis 中失败: {}", e.getMessage(), e);
        }
    }

}
