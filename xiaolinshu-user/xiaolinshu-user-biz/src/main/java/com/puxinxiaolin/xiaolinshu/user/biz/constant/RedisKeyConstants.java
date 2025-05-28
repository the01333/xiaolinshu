package com.puxinxiaolin.xiaolinshu.user.biz.constant;

/**
 * @Description: redis-key常量类
 * @Author: YCcLin
 * @Date: 2025/5/21 8:13
 */
public class RedisKeyConstants {
    
    // 小林书全局 ID 生成器 KEY
    public static final String XIAOLINSHU_ID_GENERATOR_KEY = "xiaolinshu.id.generator";

    // 用户角色数据 KEY 前缀
    private static final String USER_ROLES_KEY_PREFIX = "user:roles:";

    // 角色对应的权限集合 KEY 前缀
    private static final String ROLE_PERMISSIONS_KEY_PREFIX = "role:permissions:";

    // 用户信息数据 KEY 前缀
    private static final String USER_INFO_KEY_PREFIX = "user:info:";
    
    public static String buildUserRoleKey(Long userId) {
        return USER_ROLES_KEY_PREFIX + userId;
    }
    
    public static String buildRolePermissionsKey(String roleKey) {
        return ROLE_PERMISSIONS_KEY_PREFIX + roleKey;
    }

    public static String buildUserInfoKey(Long userId) {
        return USER_INFO_KEY_PREFIX + userId;
    }
    
}
