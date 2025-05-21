package com.puxinxiaolin.xiaolinshu.auth.model.vo.verificationcode;

import com.puxinxiaolin.framework.common.validator.PhoneNumber;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
public class SendVerificationCodeReqVO  {
    
    @PhoneNumber
    @NotBlank(message = "手机号不能为空")
    private String phone;
    
}
