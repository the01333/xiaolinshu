package com.puxinxiaolin.xiaolinshu.note.biz.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class LikeUnLikeNoteMqDTO {
    
    private Long userId; 
    
    private Long noteId;
    
    // 0: 取消点赞, 1: 点赞
    private Integer type;
    
    private LocalDateTime createTime;
    
    private Long noteCreatorId;
    
}
