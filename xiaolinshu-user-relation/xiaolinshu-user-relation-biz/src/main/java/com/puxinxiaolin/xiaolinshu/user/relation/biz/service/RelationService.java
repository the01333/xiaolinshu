package com.puxinxiaolin.xiaolinshu.user.relation.biz.service;

import com.puxinxiaolin.framework.common.response.Response;
import com.puxinxiaolin.xiaolinshu.user.relation.biz.model.vo.FollowUserReqVO;
import com.puxinxiaolin.xiaolinshu.user.relation.biz.model.vo.UnfollowUserReqVO;

public interface RelationService {

    /**
     * 关注用户
     *
     * @param request
     * @return
     */
    Response<?> follow(FollowUserReqVO request);

    /**
     * 取关用户
     *
     * @param request
     * @return
     */
    Response<?> unfollow(UnfollowUserReqVO request);

}
