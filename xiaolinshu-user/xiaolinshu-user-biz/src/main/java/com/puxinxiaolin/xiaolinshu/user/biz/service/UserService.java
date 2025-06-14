package com.puxinxiaolin.xiaolinshu.user.biz.service;

import com.puxinxiaolin.framework.common.response.Response;
import com.puxinxiaolin.xiaolinshu.user.api.dto.req.*;
import com.puxinxiaolin.xiaolinshu.user.api.dto.resp.FindUserByIdRspDTO;
import com.puxinxiaolin.xiaolinshu.user.api.dto.resp.FindUserByPhoneRspDTO;
import com.puxinxiaolin.xiaolinshu.user.biz.model.vo.UpdateUserInfoReqVO;

import java.util.List;

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

    /**
     * 根据用户 ID 查询用户信息
     *
     * @param request
     * @return
     */
    Response<FindUserByIdRspDTO> findById(FindUserByIdReqDTO request);

    /**
     * 批量查询用户信息
     *
     * @param request
     * @return
     */
    Response<List<FindUserByIdRspDTO>> findByIds(FindUsersByIdsReqDTO request);

}
