package com.puxinxiaolin.xiaolinshu.note.biz.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Objects;

@AllArgsConstructor
@Getter
public enum NoteCollectLuaResultEnum {
    
    // bloom 或 zset 不存在
    NOT_EXIST(-1L),
    // 已收藏
    NOTE_COLLECTED(1L),
    // 收藏成功
    NOTE_COLLECTED_SUCCESS(0L),
    ;
    
    private final Long code;
    
    public static NoteCollectLuaResultEnum valueOf(Long code) {
        for (NoteCollectLuaResultEnum value : NoteCollectLuaResultEnum.values()) {
            if (Objects.equals(value.getCode(), code)) {
                return value;
            }
        }
        
        return null;
    }
    
}
