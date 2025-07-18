package com.puxinxiaolin.xiaolinshu.search.model.vo;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SearchUserReqVO {
    
    @NotBlank(message = "搜索关键字不能为空")
    private String keyword;
    
    @Min(value = 1, message = "页码不能小于 1")
    private Integer pageNo = 1;
    
}
