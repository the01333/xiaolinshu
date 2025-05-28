package com.puxinxiaolin.xiaolinshu.note.biz.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Objects;

@Getter
@AllArgsConstructor
public enum NoteVisibleEnum {
    
    PUBLIC(0),
    PRIVATE(1),
    ;
    
    private final Integer code;

}
