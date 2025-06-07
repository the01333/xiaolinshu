package com.puxinxiaolin.xiaolinshu.user.relation.biz.service;

import com.puxinxiaolin.framework.common.response.PageResponse;
import com.puxinxiaolin.framework.common.response.Response;
import com.puxinxiaolin.xiaolinshu.user.relation.biz.model.vo.*;

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

    /**
     * 查询用户关注列表
     *
     * @param request
     * @return
     */
    PageResponse<FindFollowingUserRspVO> findFollowingList(FindFollowingListReqVO request);

    /**
     * 查询用户粉丝列表
     *
     * @param request
     * @return
     */
    PageResponse<FindFansUserRspVO> findFansList(FindFansListReqVO request);

}
