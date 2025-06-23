package com.puxinxiaolin.xiaolinshu.count.biz.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Objects;

@Getter
@AllArgsConstructor
public enum LikeUnlikeNoteTypeEnum {

    LIKE(1),
    UNLIKE(0),
    ;

    private final Integer code;

    public static LikeUnlikeNoteTypeEnum valueOf(Integer code) {
        for (LikeUnlikeNoteTypeEnum typeEnum : LikeUnlikeNoteTypeEnum.values()) {
            if (Objects.equals(code, typeEnum.getCode())) {
                return typeEnum;
            }
        }
        return null;
    }


}