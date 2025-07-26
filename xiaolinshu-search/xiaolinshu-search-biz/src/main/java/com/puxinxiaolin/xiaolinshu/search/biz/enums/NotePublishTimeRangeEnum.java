package com.puxinxiaolin.xiaolinshu.search.biz.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum NotePublishTimeRangeEnum {
    
    DAY(0),
    WEEK(1),
    HALF_YEAR(2),
    ;
    
    private final Integer code;
    
    public static NotePublishTimeRangeEnum valueOf(Integer code) {
        for (NotePublishTimeRangeEnum value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        return null;
    }
    
}
