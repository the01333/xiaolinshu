package com.puxinxiaolin.xiaolinshu.comment.biz.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Objects;

@Getter
@AllArgsConstructor
public enum CommentUnlikeLuaResultEnum {
    // 布隆过滤器不存在
    NOT_EXIST(-1L),
    // 评论已点赞
    COMMENT_LIKED(1L),
    // 评论未点赞
    COMMENT_NOT_LIKED(0L),
    ;

    private final Long code;

    /**
     * 根据类型 code 获取对应的枚举
     *
     * @param code
     * @return
     */
    public static CommentUnlikeLuaResultEnum valueOf(Long code) {
        for (CommentUnlikeLuaResultEnum commentLikeLuaResultEnum : CommentUnlikeLuaResultEnum.values()) {
            if (Objects.equals(code, commentLikeLuaResultEnum.getCode())) {
                return commentLikeLuaResultEnum;
            }
        }
        return null;
    }
}
