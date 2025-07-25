package com.puxinxiaolin.xiaolinshu.search.biz.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum UserStatusEnum {
    
    ENABLE(0),
    DISABLED(1),
    ;
    
    private final Integer code;
    
}
