package com.puxinxiaolin.framework.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum DeletedEnum {
    
    TES(true),
    NO(false);
    
    private final Boolean value;
    
}
