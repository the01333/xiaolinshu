package com.puxinxiaolin.xiaolinshu.count.biz.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AggregationCountLikeUnLikeNoteMqDTO {
    
    private Long noteId;
    
    private Long creatorId;

    /**
     * 聚合后的数据
     */
    private Integer count;
    
}
