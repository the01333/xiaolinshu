package com.puxinxiaolin.framework.biz.context.interceptor;

import com.puxinxiaolin.framework.biz.context.holder.LoginUserContextHolder;
import com.puxinxiaolin.framework.common.constant.GlobalConstants;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import lombok.extern.slf4j.Slf4j;

import java.util.Objects;

/**
 * @Description: Feign 请求拦截器, 把 userId 进行传递
 * @Author: YCcLin
 * @Date: 2025/5/25 16:33
 */
@Slf4j
public class FeignRequestInterceptor implements RequestInterceptor {

    @Override
    public void apply(RequestTemplate requestTemplate) {
        Long userId = LoginUserContextHolder.getUserId();
        if (Objects.nonNull(userId)) {
            requestTemplate.header(GlobalConstants.USER_ID, userId.toString());

            log.info("########## feign 请求设置请求头 userId: {}", userId);
        }
    }

}
