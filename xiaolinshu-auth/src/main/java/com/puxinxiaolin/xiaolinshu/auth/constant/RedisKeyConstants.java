package com.puxinxiaolin.xiaolinshu.auth.constant;

/**
 * @Description: redis-key常量类
 * @Author: YCcLin
 * @Date: 2025/5/21 8:13
 */
public class RedisKeyConstants {

    public static final String VERIFICATION_CODE_KEY_PREFIX = "verification_code:";

    // 小林书全局 ID 生成器 KEY
    public static final String XIAOLINSHU_ID_GENERATOR_KEY = "xiaolinshu.id.generator";

    // 用户角色数据 KEY 前缀
    private static final String USER_ROLES_KEY_PREFIX = "user:roles:";

    // 角色对应的权限集合 KEY 前缀
    private static final String ROLE_PERMISSIONS_KEY_PREFIX = "role:permissions:";


    public static String buildVerificationCodeKey(String phone) {
        return VERIFICATION_CODE_KEY_PREFIX + phone;
    }

    public static String buildUserRoleKey(String phone) {
        return USER_ROLES_KEY_PREFIX + phone;
    }
    
    public static String buildRolePermissionsKey(Long roleId) {
        return ROLE_PERMISSIONS_KEY_PREFIX + roleId;
    }

}
