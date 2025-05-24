package com.puxinxiaolin.xiaolinshu.user.biz.service;

import com.puxinxiaolin.framework.common.response.Response;
import com.puxinxiaolin.xiaolinshu.user.biz.model.vo.UpdateUserInfoReqVO;

public interface UserService {

    /**
     * 更新用户信息
     *
     * @param request
     * @return
     */
    Response<?> updateUserInfo(UpdateUserInfoReqVO request);

}
