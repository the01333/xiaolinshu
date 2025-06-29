package com.puxinxiaolin.xiaolinshu.note.biz.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Objects;

@AllArgsConstructor
@Getter
public enum NoteUnCollectLuaResultEnum {
    
    // bloom 不存在
    NOT_EXIST(-1L),
    // 已收藏
    NOTE_COLLECTED(1L),
    // 未收藏
    NOTE_NOT_COLLECTED(0L),
    ;
    
    private final Long code;
    
    public static NoteUnCollectLuaResultEnum valueOf(Long code) {
        for (NoteUnCollectLuaResultEnum value : NoteUnCollectLuaResultEnum.values()) {
            if (Objects.equals(value.getCode(), code)) {
                return value;
            }
        }
        
        return null;
    }
    
}
