package com.puxinxiaolin.xiaolinshu.comment.biz.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Description: 查询二级评论分页数据响应 VO
 * @Author: YCcLin
 * @Date: 2025/8/24 17:36
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class FindChildCommentPageListRspVO {

    private Long commentId;

    private Long userId;

    private String avatar;

    private String nickname;

    private String content;

    private String imageUrl;

    private String createTime;

    private Long likeTotal;

    private String replyUserName;

    private Long replyUserId;

}
