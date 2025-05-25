package com.puxinxiaolin.xiaolinshu.auth.rpc;

import com.puxinxiaolin.framework.common.response.Response;
import com.puxinxiaolin.xiaolinshu.user.api.api.UserFeignApi;
import com.puxinxiaolin.xiaolinshu.user.api.dto.req.RegisterUserReqDTO;
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

}
