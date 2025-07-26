package com.puxinxiaolin.xiaolinshu.search.biz.controller;

import com.puxinxiaolin.framework.biz.operationlog.aspect.ApiOperationLog;
import com.puxinxiaolin.framework.common.response.PageResponse;
import com.puxinxiaolin.framework.common.response.Response;
import com.puxinxiaolin.xiaolinshu.search.api.dto.RebuildUserDocumentReqDTO;
import com.puxinxiaolin.xiaolinshu.search.biz.model.vo.SearchUserReqVO;
import com.puxinxiaolin.xiaolinshu.search.biz.model.vo.SearchUserRespVO;
import com.puxinxiaolin.xiaolinshu.search.biz.service.UserService;
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

    // ===================================== 对其他服务提供的接口 =====================================
    @PostMapping("/user/document/rebuild")
    @ApiOperationLog(description = "用户文档重建")
    public Response<?> rebuildDocument(@Validated @RequestBody RebuildUserDocumentReqDTO request) {
        return userService.rebuildDocument(request);
    }
    
}