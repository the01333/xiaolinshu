package com.puxinxiaolin.xiaolinshu.comment.biz.domain.dataobject;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CommentDO {
    
    private Long id;

    private Long noteId;

    private Long userId;

    private String contentUuid;

    private Boolean isContentEmpty;

    private String imageUrl;

    private Integer level;

    private Long replyTotal;

    private Long likeTotal;

    private Long parentId;

    private Long replyCommentId;

    private Long replyUserId;

    private Boolean isTop;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    /**
     * 子评论数
     */
    private Long childCommentTotal;

    /**
     * 最早回复的子评论id
     */
    private Long firstReplyCommentId;

    /**
     * 热度值
     */
    private Double heat;
    
}