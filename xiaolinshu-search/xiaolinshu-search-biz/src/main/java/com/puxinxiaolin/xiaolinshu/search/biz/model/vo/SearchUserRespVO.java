package com.puxinxiaolin.xiaolinshu.search.biz.model.vo;

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

    /**
     * 昵称: 关键词高亮
     */
    private String highlightNickname;
    
    private String avatar;
    
    private String xiaolinshuId;
    
    private Integer noteTotal;
    
    private String fansTotal;
    
}
