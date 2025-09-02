package com.puxinxiaolin.xiaolinshu.comment.biz.domain.dataobject;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CommentLikeDO {
    
    private Long id;

    private Long userId;

    private Long commentId;

    private Date createTime;

}