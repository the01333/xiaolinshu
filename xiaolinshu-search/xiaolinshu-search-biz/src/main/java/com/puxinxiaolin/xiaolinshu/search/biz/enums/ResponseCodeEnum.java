package com.puxinxiaolin.xiaolinshu.search.biz.enums;

import com.puxinxiaolin.framework.common.exception.BaseExceptionInterface;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @Description: 响应异常码
 * @Author: YCcLin
 * @Date: 2025/7/15 23:25
 */
@Getter
@AllArgsConstructor
public enum ResponseCodeEnum implements BaseExceptionInterface {

    // ----------- 通用异常状态码 -----------
    SYSTEM_ERROR("SEARCH-10000", "出错啦，后台小哥正在努力修复中..."),
    PARAM_NOT_VALID("SEARCH-10001", "参数错误"),

    // ----------- 业务异常状态码 -----------
    ;

    private final String errorCode;
    private final String errorMessage;

}
