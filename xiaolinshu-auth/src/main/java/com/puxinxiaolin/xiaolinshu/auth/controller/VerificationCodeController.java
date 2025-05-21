package com.puxinxiaolin.xiaolinshu.auth.controller;

import com.puxinxiaolin.framework.common.response.Response;
import com.puxinxiaolin.xiaolinshu.auth.model.vo.verificationcode.SendVerificationCodeReqVO;
import com.puxinxiaolin.xiaolinshu.auth.service.VerificationCodeService;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class VerificationCodeController {
    
    @Resource
    private VerificationCodeService verificationCodeService;
    
    @PostMapping("/verification/code/send")
    public Response<?> send(@RequestBody @Validated SendVerificationCodeReqVO request) {
        return verificationCodeService.send(request);
    } 
    
}
