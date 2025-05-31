package com.puxinxiaolin.xiaolinshu.note.biz.model.vo;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TopNoteReqVO {

    @NotNull(message = "笔记 ID 不能为空")
    private Long id;

    /**
     * 置顶状态: true 为置顶, false 为取消置顶
     */
    @NotNull(message = "置顶状态不能为空")
    private Boolean isTop;
    
}
