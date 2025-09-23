package com.puxinxiaolin.xiaolinshu.comment.biz.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum CommentLevelEnum {
    
    // 一级评论
    ONE(1),
    // 二级评论
    TWO(2),
    ;
    
    private final Integer code;
    
    public static CommentLevelEnum valueOf(Integer code) {
        for (CommentLevelEnum value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        return null;
    }
    
}
