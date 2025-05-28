package com.puxinxiaolin.xiaolinshu.note.biz.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum NoteStatusEnum {
    
    BE_EXAMINE(0),
    NORMAL(1),
    DELETED(2),
    DOWNED(3),
    ;
    
    private final Integer code;

}
