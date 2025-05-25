package com.puxinxiaolin.xiaolinshu.user.api.api;

import com.puxinxiaolin.framework.common.response.Response;
import com.puxinxiaolin.xiaolinshu.user.api.constant.ApiConstants;
import com.puxinxiaolin.xiaolinshu.user.api.dto.req.FindUserByPhoneReqDTO;
import com.puxinxiaolin.xiaolinshu.user.api.dto.req.RegisterUserReqDTO;
import com.puxinxiaolin.xiaolinshu.user.api.dto.req.UpdateUserPasswordReqDTO;
import com.puxinxiaolin.xiaolinshu.user.api.dto.resp.FindUserByPhoneRspDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = ApiConstants.SERVICE_NAME)
public interface UserFeignApi {

    String PREFIX = "/user";

    @PostMapping(PREFIX + "/register")
    Response<Long> register(@RequestBody RegisterUserReqDTO request);

    @PostMapping(PREFIX + "/findByPhone")
    Response<FindUserByPhoneRspDTO> findByPhone(@RequestBody FindUserByPhoneReqDTO request);

    @PostMapping(PREFIX + "/password/update")
    Response<?> updatePassword(@RequestBody UpdateUserPasswordReqDTO request);
    
}
