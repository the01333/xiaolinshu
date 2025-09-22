package com.puxinxiaolin.xiaolinshu.comment.biz.model.vo;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Description: 取消评论点赞 VO
 * @Author: YCcLin
 * @Date: 2025/9/22 21:45
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UnLikeCommentReqVO {

    @NotNull(message = "评论 ID 不能为空")
    private Long commentId;

}
