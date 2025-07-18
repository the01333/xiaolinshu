package com.puxinxiaolin.xiaolinshu.search.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SearchUserRespVO {
    
    private Long userId;
    
    private String nickName;
    
    private String avatar;
    
    private String xiaolinshuId;
    
    private Integer noteTotal;
    
    private Integer fansTotal;
    
}
