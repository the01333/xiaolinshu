package com.puxinxiaolin.xiaolinshu.count.biz.domain.dataobject;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

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

    private Byte level;

    private Long replyTotal;

    private Long likeTotal;

    private Long parentId;

    private Long replyCommentId;

    private Long replyUserId;

    private Byte isTop;

    private Date createTime;

    private Date updateTime;

    private Long childCommentTotal;

}