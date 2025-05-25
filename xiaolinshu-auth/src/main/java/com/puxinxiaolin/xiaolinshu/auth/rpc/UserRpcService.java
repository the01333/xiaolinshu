package com.puxinxiaolin.xiaolinshu.auth.rpc;

import com.puxinxiaolin.framework.common.response.Response;
import com.puxinxiaolin.xiaolinshu.user.api.api.UserFeignApi;
import com.puxinxiaolin.xiaolinshu.user.api.dto.req.FindUserByPhoneReqDTO;
import com.puxinxiaolin.xiaolinshu.user.api.dto.req.RegisterUserReqDTO;
import com.puxinxiaolin.xiaolinshu.user.api.dto.req.UpdateUserPasswordReqDTO;
import com.puxinxiaolin.xiaolinshu.user.api.dto.resp.FindUserByPhoneRspDTO;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

@Component
public class UserRpcService {

    @Resource
    private UserFeignApi userFeignApi;

    /**
     * 注册用户
     *
     * @param phone
     * @return
     */
    public Long registerUser(String phone) {
        RegisterUserReqDTO dto = new RegisterUserReqDTO();
        dto.setPhone(phone);

        Response<Long> response = userFeignApi.register(dto);
        if (!response.isSuccess()) {
            return null;
        }

        return response.getData();
    }

    /**
     * 根据手机号查找用户
     *
     * @param phone
     * @return
     */
    public FindUserByPhoneRspDTO findUserByPhone(String phone) {
        FindUserByPhoneReqDTO dto = new FindUserByPhoneReqDTO();
        dto.setPhone(phone);

        Response<FindUserByPhoneRspDTO> response = userFeignApi.findByPhone(dto);
        if (!response.isSuccess()) {
            return null;
        }

        return response.getData();
    }

    /**
     * 修改密码
     *
     * @param encodePassword
     */
    public void updatePassword(String encodePassword) {
        UpdateUserPasswordReqDTO dto = new UpdateUserPasswordReqDTO();
        dto.setEncodePassword(encodePassword);

        userFeignApi.updatePassword(dto);
    }

}
