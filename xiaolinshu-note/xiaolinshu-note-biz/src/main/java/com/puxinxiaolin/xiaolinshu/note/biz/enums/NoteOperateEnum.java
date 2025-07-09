package com.puxinxiaolin.xiaolinshu.note.biz.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum NoteOperateEnum {
    
    // 发布
    PUBLISH(1),
    // 删除
    DELETE(0),
    ;
    
    private final Integer code;
    
}
