package com.puxinxiaolin.xiaolinshu.user.relation.biz.controller;

import com.puxinxiaolin.framework.biz.operationlog.aspect.ApiOperationLog;
import com.puxinxiaolin.framework.common.response.Response;
import com.puxinxiaolin.xiaolinshu.user.relation.biz.model.vo.FollowUserReqVO;
import com.puxinxiaolin.xiaolinshu.user.relation.biz.model.vo.UnfollowUserReqVO;
import com.puxinxiaolin.xiaolinshu.user.relation.biz.service.RelationService;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/relation")
public class RelationController {

    @Resource
    private RelationService relationService;

    @PostMapping("/follow")
    @ApiOperationLog(description = "关注用户")
    public Response<?> follow(@Validated @RequestBody FollowUserReqVO request) {
        return relationService.follow(request);
    }

    @PostMapping("/unfollow")
    @ApiOperationLog(description = "取关用户")
    public Response<?> unfollow(@Validated @RequestBody UnfollowUserReqVO request) {
        return relationService.unfollow(request);
    }

}
