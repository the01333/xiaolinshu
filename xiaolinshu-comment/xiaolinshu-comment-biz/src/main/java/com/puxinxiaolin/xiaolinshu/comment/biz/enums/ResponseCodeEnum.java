package com.puxinxiaolin.xiaolinshu.comment.biz.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ResponseCodeEnum {

    // ----------- 通用异常状态码 -----------
    SYSTEM_ERROR("COMMENT-10000", "出错啦, 后台小哥正在努力修复中..."),
    PARAM_NOT_VALID("COMMENT-10001", "参数错误"),

    // ----------- 业务异常状态码 -----------
    ;
    
    private final String errorCode;
    private final String errorMessage;
    
}
