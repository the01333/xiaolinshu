package com.puxinxiaolin.xiaolinshu.count.biz.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Description: 点赞、取消点赞评论 MQ 实体
 * @Author: YCcLin
 * @Date: 2025/9/22 22:56
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CountLikeUnlikeCommentMqDTO {

    private Long userId;

    private Long commentId;

    /**
     * 0: 取消点赞， 1：点赞
     */
    private Integer type;
    
}

