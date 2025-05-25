package com.puxinxiaolin.xiaolinshu.user.biz.service;

import com.puxinxiaolin.framework.common.response.Response;
import com.puxinxiaolin.xiaolinshu.user.api.dto.req.FindUserByPhoneReqDTO;
import com.puxinxiaolin.xiaolinshu.user.api.dto.req.RegisterUserReqDTO;
import com.puxinxiaolin.xiaolinshu.user.api.dto.req.UpdateUserPasswordReqDTO;
import com.puxinxiaolin.xiaolinshu.user.api.dto.resp.FindUserByPhoneRspDTO;
import com.puxinxiaolin.xiaolinshu.user.biz.model.vo.UpdateUserInfoReqVO;

public interface UserService {

    /**
     * 更新用户信息
     *
     * @param request
     * @return
     */
    Response<?> updateUserInfo(UpdateUserInfoReqVO request);

    /**
     * 用户注册
     *
     * @param request
     * @return
     */
    Response<Long> register(RegisterUserReqDTO request);

    /**
     * 根据手机号查找用户
     *
     * @param request
     * @return
     */
    Response<FindUserByPhoneRspDTO> findByPhone(FindUserByPhoneReqDTO request);

    /**
     * 修改密码
     *
     * @param request
     * @return
     */
    Response<?> updatePassword(UpdateUserPasswordReqDTO request);
    
}
