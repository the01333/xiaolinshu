package com.puxinxiaolin.xiaolinshu.oss.biz.enums;

import com.puxinxiaolin.framework.common.exception.BaseExceptionInterface;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ResponseCodeEnum implements BaseExceptionInterface {

    // ----------- 通用异常状态码 -----------
    SYSTEM_ERROR("OSS-10000", "系统繁忙，请稍后再试"),
    PARAM_NOT_VALID("OSS-10001", "参数错误"),


    // ----------- 业务异常状态码 -----------
    ;

    // 异常码
    private final String errorCode;
    // 错误信息
    private final String errorMessage;
    
}
