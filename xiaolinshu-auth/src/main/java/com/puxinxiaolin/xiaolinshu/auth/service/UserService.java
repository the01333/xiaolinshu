package com.puxinxiaolin.xiaolinshu.auth.service;

import com.puxinxiaolin.framework.common.response.Response;
import com.puxinxiaolin.xiaolinshu.auth.model.vo.user.UserLoginReqVO;

public interface UserService {

    /**
     * 登录与注册
     *
     * @param userLoginReqVO
     * @return
     */
    Response<String> loginAndRegister(UserLoginReqVO userLoginReqVO);

}
