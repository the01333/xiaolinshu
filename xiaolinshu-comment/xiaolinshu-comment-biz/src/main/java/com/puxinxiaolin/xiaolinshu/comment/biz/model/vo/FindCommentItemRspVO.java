package com.puxinxiaolin.xiaolinshu.comment.biz.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Description: 评论项返回 VO
 * @Author: YCcLin
 * @Date: 2025/8/17 22:55
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class FindCommentItemRspVO {

    private Long commentId;

    private Long userId;

    private String avatar;

    private String nickname;

    private String content;

    private String imageUrl;

    private String createTime;

    private Long likeTotal;

    /**
     * 子评论总数
     */
    private Long childCommentTotal;

    /**
     * 最早回复的评论
     */
    private FindCommentItemRspVO firstReplyComment;

}