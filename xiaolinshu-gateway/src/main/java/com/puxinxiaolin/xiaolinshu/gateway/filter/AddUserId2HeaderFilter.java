package com.puxinxiaolin.xiaolinshu.gateway.filter;

import cn.dev33.satoken.stp.StpUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * @Description: 转发请求时，将 userId 添加到 Header 中，传递给下游服务。GlobalFilter 是全局过滤器接口，会对所有通过网关的请求生效
 * @Author: YCcLin
 * @Date: 2025/5/22 22:03
 */
@Component
@Slf4j
public class AddUserId2HeaderFilter implements GlobalFilter {

    private static final String HEADER_USER_ID = "userId";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        log.info("==================> TokenConvertFilter");

        Long userId = null;
        try {
            userId = StpUtil.getLoginIdAsLong();
        } catch (Exception e) {
            return chain.filter(exchange);
        }

        log.info("## 当前登录的用户 ID: {}", userId);

        Long finalUserId = userId;
        exchange.mutate()
                .request(request -> request.header(HEADER_USER_ID, String.valueOf(finalUserId)))
                .build();
        
        return chain.filter(exchange);
    }

}
