package com.puxinxiaolin.xiaolinshu.note.biz.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Objects;

@Getter
@AllArgsConstructor
public enum NoteLikeLuaResultEnum {

    NOTE_LIKE_SUCCESS(0L),
    NOT_EXIST(-1L),
    NOTE_LIKED(1L),
    ;

    private final Long code;

    public static NoteLikeLuaResultEnum valueOf(Long code) {
        for (NoteLikeLuaResultEnum value : NoteLikeLuaResultEnum.values()) {
            if (Objects.equals(value.getCode(), code)) {
                return value;
            }
        }
        return null;
    }

}
