package com.puxinxiaolin.xiaolinshu.search.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum NoteSortTypeEnum {

    LATEST(0),
    MOST_LIKE(1),
    MOST_COMMENT(2),
    MOST_COLLECT(3),
    ;

    private final Integer code;
    
    public static NoteSortTypeEnum valueOf(Integer code) {
        for (NoteSortTypeEnum value : NoteSortTypeEnum.values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        return null;
    }

}
