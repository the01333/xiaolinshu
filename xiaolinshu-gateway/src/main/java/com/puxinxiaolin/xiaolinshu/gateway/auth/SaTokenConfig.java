package com.puxinxiaolin.xiaolinshu.gateway.auth;

import cn.dev33.satoken.context.SaHolder;
import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotPermissionException;
import cn.dev33.satoken.exception.NotRoleException;
import cn.dev33.satoken.reactor.filter.SaReactorFilter;
import cn.dev33.satoken.router.SaRouter;
import cn.dev33.satoken.stp.StpUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @Description: 权限认证配置类
 * @Author: YCcLin
 * @Date: 2025/5/22 14:52
 */
@Configuration
@Slf4j
public class SaTokenConfig {

    @Bean
    public SaReactorFilter getSaReactorFilter() {
        return new SaReactorFilter()
                .addInclude("/**")
                .setAuth(obj -> {
                    log.info("==================> SaReactorFilter, Path: {}", SaHolder.getRequest().getRequestPath());
                    
                    // 排除 /user/doLogin 用于登录
                    SaRouter.match("/**")
                            .notMatch("/auth/user/login")
                            .notMatch("/auth/verification/code/send")
                            .check(r -> StpUtil.checkLogin())
                    ;
                    
//                    SaRouter.match("/auth/user/logout", r -> StpUtil.checkRole("admin"))
                    SaRouter.match("/auth/user/logout", r -> StpUtil.checkPermission("app:note:publish"))
                    ;
                    
                }).setError(e -> {
                    if (e instanceof NotLoginException) {
                        throw new NotLoginException(e.getMessage(), null, null);
                    } else if (e instanceof NotPermissionException || e instanceof NotRoleException) {
                        throw new NotPermissionException(e.getMessage());
                    } else {
                        throw new RuntimeException(e.getMessage());
                    }
                })
                ;
    }

}
