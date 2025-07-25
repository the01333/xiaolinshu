package com.puxinxiaolin.xiaolinshu.search.biz.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum NoteStatusEnum {
    
    BE_EXAMING(0),  // 待审核
    NORMAL(1),  // 正常展示
    DELETED(2),  // 被删除
    DOWNED(3),  // 被下架
    ;
    
    private final Integer code;
    
}
