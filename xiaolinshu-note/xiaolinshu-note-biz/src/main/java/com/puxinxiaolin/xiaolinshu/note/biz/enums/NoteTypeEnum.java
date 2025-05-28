package com.puxinxiaolin.xiaolinshu.note.biz.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Objects;

@Getter
@AllArgsConstructor
public enum NoteTypeEnum {
    
    IMAGE_TEXT(0, "图文"),
    VIDEO(1, "视频")
    ;
    
    private final Integer code;
    private final String message;
    
    public static boolean isValid(Integer code) {
        for (NoteTypeEnum value : NoteTypeEnum.values()) {
            if (Objects.equals(value.getCode(), code)) {
                return true;
            }
        }
        
        return false;
    }
    
    public static NoteTypeEnum valueOf(Integer code) {
        for (NoteTypeEnum value : NoteTypeEnum.values()) {
            if (Objects.equals(value.getCode(), code)) {
                return value;
            }
        }

        return null;
    }

}
