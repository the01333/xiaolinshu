package com.puxinxiaolin.xiaolinshu.auth.controller;

import com.puxinxiaolin.framework.biz.operationlog.aspect.ApiOperationLog;
import com.puxinxiaolin.framework.common.response.Response;
import com.puxinxiaolin.xiaolinshu.auth.model.vo.user.UpdatePasswordReqVO;
import com.puxinxiaolin.xiaolinshu.auth.model.vo.user.UserLoginReqVO;
import com.puxinxiaolin.xiaolinshu.auth.service.AuthService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@Slf4j
public class AuthController {
    
    @Resource
    private AuthService authService;
    
    @PostMapping("/login")
    @ApiOperationLog(description = "用户登录/注册")
    public Response<String> loginAndRegister(@RequestBody @Validated UserLoginReqVO request) {
        return authService.loginAndRegister(request);
    }

    @PostMapping("/logout")
    @ApiOperationLog(description = "账号登出")
    public Response<?> logout() {
        return authService.logout();
    }

    @PostMapping("/password/update")
    @ApiOperationLog(description = "修改密码")
    public Response<?> updatePassword(@RequestBody @Validated UpdatePasswordReqVO request) {
        return authService.updatePassword(request);
    }

}
