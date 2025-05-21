package com.puxinxiaolin.xiaolinshu.auth.service;

import com.puxinxiaolin.framework.common.response.Response;
import com.puxinxiaolin.xiaolinshu.auth.model.vo.verificationcode.SendVerificationCodeReqVO;

public interface VerificationCodeService {

    /**
     * 发送短信验证码
     *
     * @param request
     * @return
     */
    Response<?> send(SendVerificationCodeReqVO request);

}
