package com.puxinxiaolin.xiaolinshu.search.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SearchNoteRespVO {
    
    private Long noteId;
    
    private String cover;
    
    private String avatar;
    
    private String title;

    /**
     * 标题: 关键词高亮
     */
    private String highlightTitle;
    
    private String nickname;
    
    private LocalDateTime updateTime;

    /**
     * 被点赞总数
     */
    private String likeTotal;
    
}
