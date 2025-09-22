package com.puxinxiaolin.xiaolinshu.count.biz.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Description: 聚合后计数：点赞、取消点赞评论 MQ 实体
 * @Author: YCcLin
 * @Date: 2025/9/22 22:56
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AggregationCountLikeUnlikeCommentMqDTO {

    /**
     * 评论 ID
     */
    private Long commentId;

    /**
     * 聚合后的计数
     */
    private Integer count;

}

