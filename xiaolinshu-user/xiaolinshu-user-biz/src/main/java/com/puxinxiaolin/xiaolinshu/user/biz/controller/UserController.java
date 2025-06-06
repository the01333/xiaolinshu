package com.puxinxiaolin.xiaolinshu.user.biz.controller;

import com.puxinxiaolin.framework.biz.operationlog.aspect.ApiOperationLog;
import com.puxinxiaolin.framework.common.response.Response;
import com.puxinxiaolin.xiaolinshu.user.api.dto.req.*;
import com.puxinxiaolin.xiaolinshu.user.api.dto.resp.FindUserByIdRspDTO;
import com.puxinxiaolin.xiaolinshu.user.api.dto.resp.FindUserByPhoneRspDTO;
import com.puxinxiaolin.xiaolinshu.user.biz.model.vo.UpdateUserInfoReqVO;
import com.puxinxiaolin.xiaolinshu.user.biz.service.UserService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@Slf4j
@RequestMapping("/user")
public class UserController {

    @Resource
    private UserService userService;

    /**
     * 修改用户信息
     *
     * @param request
     * @return
     */
    @PostMapping(value = "/update", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Response<?> updateUserInfo(UpdateUserInfoReqVO request) {
        return userService.updateUserInfo(request);
    }

    @PostMapping("/register")
    @ApiOperationLog(description = "用户注册")
    public Response<Long> register(@Validated @RequestBody RegisterUserReqDTO request) {
        return userService.register(request);
    }

    @PostMapping("/findByPhone")
    @ApiOperationLog(description = "根据手机号查找用户")
    public Response<FindUserByPhoneRspDTO> findByPhone(@Validated @RequestBody FindUserByPhoneReqDTO request) {
        return userService.findByPhone(request);
    }

    @PostMapping("/password/update")
    @ApiOperationLog(description = "修改密码")
    public Response<?> updatePassword(@Validated @RequestBody UpdateUserPasswordReqDTO request) {
        return userService.updatePassword(request);
    }

    @PostMapping("/findById")
    @ApiOperationLog(description = "查询用户信息")
    public Response<FindUserByIdRspDTO> findById(@Validated @RequestBody FindUserByIdReqDTO request) {
        return userService.findById(request);
    }

    @PostMapping("/findByIds")
    @ApiOperationLog(description = "批量查询用户信息")
    public Response<List<FindUserByIdRspDTO>> findByIds(@Validated @RequestBody FindUsersByIdsReqDTO request) {
        return userService.findByIds(request);
    }
    
}
