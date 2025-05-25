package com.puxinxiaolin.xiaolinshu.auth.service;

import com.puxinxiaolin.framework.common.response.Response;
import com.puxinxiaolin.xiaolinshu.auth.model.vo.user.UpdatePasswordReqVO;
import com.puxinxiaolin.xiaolinshu.auth.model.vo.user.UserLoginReqVO;

public interface AuthService {

    /**
     * 修改密码
     *
     * @param request
     * @return
     */
    Response<?> updatePassword(UpdatePasswordReqVO request);

    /**
     * 登录与注册
     *
     * @param request
     * @return
     */
    Response<String> loginAndRegister(UserLoginReqVO request);

    /**
     * 退出登录
     *
     * @return
     */
    Response<?> logout();

}
