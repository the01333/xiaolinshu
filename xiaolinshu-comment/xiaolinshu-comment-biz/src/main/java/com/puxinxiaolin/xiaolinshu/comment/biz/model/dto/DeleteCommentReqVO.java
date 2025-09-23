package com.puxinxiaolin.xiaolinshu.comment.biz.model.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Description: 删除评论 VO
 * @Author: YCcLin
 * @Date: 2025/9/23 22:41
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DeleteCommentReqVO {

    @NotNull(message = "评论 ID 不能为空")
    private Long commentId;

}

