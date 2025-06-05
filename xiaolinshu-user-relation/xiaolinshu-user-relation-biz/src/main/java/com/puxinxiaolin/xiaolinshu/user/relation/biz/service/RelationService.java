package com.puxinxiaolin.xiaolinshu.user.relation.biz.service;

import com.puxinxiaolin.framework.common.response.Response;
import com.puxinxiaolin.xiaolinshu.user.relation.biz.model.vo.FollowUserReqVO;

public interface RelationService {

    /**
     * 关注用户
     *
     * @param request
     * @return
     */
    Response<?> follow(FollowUserReqVO request);

}
