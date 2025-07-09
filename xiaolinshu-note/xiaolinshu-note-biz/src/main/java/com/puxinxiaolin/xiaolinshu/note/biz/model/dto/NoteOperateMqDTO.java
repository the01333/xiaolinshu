package com.puxinxiaolin.xiaolinshu.note.biz.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class NoteOperateMqDTO {

    private Long creatorId;

    private Long noteId;

    /**
     * 操作类型: 0 - 笔记删除; 1: 笔记发布
     */
    private Integer type;

}

