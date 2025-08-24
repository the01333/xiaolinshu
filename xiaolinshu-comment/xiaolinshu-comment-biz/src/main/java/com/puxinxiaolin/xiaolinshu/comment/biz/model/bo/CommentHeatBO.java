package com.puxinxiaolin.xiaolinshu.comment.biz.model.bo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CommentHeatBO {

    /**
     * 评论 ID
     */
    private Long id;

    /**
     * 热度值
     */
    private Double heat;

    /**
     * 所属笔记 ID
     */
    private Long noteId;

}
