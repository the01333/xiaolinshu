package com.puxinxiaolin.xiaolinshu.note.biz.model.vo;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PublishNoteReqVO {

    @NotNull(message = "笔记类型不能为空")
    private Integer type;

    // 如果类型是图文, 这个不能为空
    private List<String> imgUris;

    // 如果类型是视频, 这个不能为空
    private String videoUri;

    private String title;

    private String content;

    private Long topicId;
    
}
