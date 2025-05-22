package com.puxinxiaolin.xiaolinshu.gateway.auth;

import cn.dev33.satoken.stp.StpInterface;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * @Description: 自定义权限验证接口扩展
 * @Author: YCcLin
 * @Date: 2025/5/22 14:52
 */
@Component
public class StpInterfaceImpl implements StpInterface {

    @Override
    public List<String> getPermissionList(Object o, String s) {
        // TODO [YCcLin 2025/5/22]: 走redis
        return Collections.emptyList();
    }

    @Override
    public List<String> getRoleList(Object o, String s) {
        // TODO [YCcLin 2025/5/22]: 走redis 
        return Collections.emptyList();
    }

}
