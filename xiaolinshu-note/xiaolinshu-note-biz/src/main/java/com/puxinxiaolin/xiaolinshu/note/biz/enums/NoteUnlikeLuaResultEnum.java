package com.puxinxiaolin.xiaolinshu.note.biz.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Objects;

@Getter
@AllArgsConstructor
public enum NoteUnlikeLuaResultEnum {

    NOT_EXIST(-1L),
    NOTE_LIKED(1L),
    NOTE_NOT_LIKED(0L),
    ;

    private final Long code;
    
    public static NoteUnlikeLuaResultEnum valueOf(Long code) {
        for (NoteUnlikeLuaResultEnum resultEnum : NoteUnlikeLuaResultEnum.values()) {
            if (Objects.equals(code, resultEnum.getCode())) {
                return resultEnum;
            }
        }
        return null;
    }
    
}

