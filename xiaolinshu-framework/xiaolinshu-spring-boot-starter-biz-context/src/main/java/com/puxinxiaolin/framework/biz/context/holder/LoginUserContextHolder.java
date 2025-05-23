package com.puxinxiaolin.framework.biz.context.holder;

import com.alibaba.ttl.TransmittableThreadLocal;
import com.puxinxiaolin.framework.common.constant.GlobalConstants;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * @Description: 用户登录上下文
 * @Author: YCcLin
 * @Date: 2025/5/23 13:40
 */
public class LoginUserContextHolder {

    private static final ThreadLocal<Map<String, Object>> LOGIN_USER_CONTEXT_THREAD_LOCAL =
            TransmittableThreadLocal.withInitial(HashMap::new);
    
    
    public static void setUserId(Object value) {
        LOGIN_USER_CONTEXT_THREAD_LOCAL.get().put(GlobalConstants.USER_ID, value);
    }
    
    public static Long getUserId() {
        Object value = LOGIN_USER_CONTEXT_THREAD_LOCAL.get().get(GlobalConstants.USER_ID);
        if (Objects.isNull(value)) {
            return null;
        }
        
        return Long.valueOf(value.toString());
    }
    
    public static void remove() {
        LOGIN_USER_CONTEXT_THREAD_LOCAL.remove();
    }

}
