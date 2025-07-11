package com.puxinxiaolin.xiaolinshu.data.align.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class LikeUnlikeNoteMqDTO {
    
    private Long noteId;

    private Long userId;
    
    /**
     * 0-取消点赞  1-点赞
     */
    private Integer type;
    
    private Long noteCreatorId;
    
    private LocalDateTime createTime;
    
}
