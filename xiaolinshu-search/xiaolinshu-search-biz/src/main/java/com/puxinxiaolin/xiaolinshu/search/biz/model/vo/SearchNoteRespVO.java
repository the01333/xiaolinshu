package com.puxinxiaolin.xiaolinshu.search.biz.model.vo;

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
    
    private String updateTime;

    /**
     * 被点赞总数
     */
    private String likeTotal;

    /**
     * 被评论总数
     */
    private String commentTotal;

    /**
     * 被收藏总数
     */
    private String collectTotal;
    
}
