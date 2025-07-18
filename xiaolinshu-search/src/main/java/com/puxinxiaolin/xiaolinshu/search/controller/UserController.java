package com.puxinxiaolin.xiaolinshu.search.controller;

import com.puxinxiaolin.framework.biz.operationlog.aspect.ApiOperationLog;
import com.puxinxiaolin.framework.common.response.PageResponse;
import com.puxinxiaolin.xiaolinshu.search.model.vo.SearchUserReqVO;
import com.puxinxiaolin.xiaolinshu.search.model.vo.SearchUserRespVO;
import com.puxinxiaolin.xiaolinshu.search.service.UserService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/search")
@Slf4j
public class UserController {

    @Resource
    private UserService userService;

    @PostMapping("/user")
    @ApiOperationLog(description = "搜索用户")
    public PageResponse<SearchUserRespVO> searchUser(@RequestBody @Validated SearchUserReqVO request) {
        return userService.searchUser(request);
    }

}