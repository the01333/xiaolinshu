package com.puxinxiaolin.xiaolinshu.auth.service.impl;

import cn.hutool.core.util.RandomUtil;
import com.puxinxiaolin.framework.common.exception.BizException;
import com.puxinxiaolin.framework.common.response.Response;
import com.puxinxiaolin.xiaolinshu.auth.constant.RedisKeyConstants;
import com.puxinxiaolin.xiaolinshu.auth.enums.ResponseCodeEnum;
import com.puxinxiaolin.xiaolinshu.auth.model.vo.verificationcode.SendVerificationCodeReqVO;
import com.puxinxiaolin.xiaolinshu.auth.service.VerificationCodeService;
import com.puxinxiaolin.xiaolinshu.auth.sms.AliyunSmsHelper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class VerificationCodeServiceImpl implements VerificationCodeService {

    @Resource
    private RedisTemplate<String, Object> redisTemplate;
    @Resource(name = "taskExecutor")
    private ThreadPoolTaskExecutor threadPoolTaskExecutor;
    @Resource
    private AliyunSmsHelper aliyunSmsHelper;

    /**
     * 发送手机验证码
     *
     * @param request
     * @return
     */
    @Override
    public Response<?> send(SendVerificationCodeReqVO request) {
        String phone = request.getPhone();

        String key = RedisKeyConstants.buildVerificationCodeKey(phone);
        Boolean isSent = redisTemplate.hasKey(key);
        if (isSent) {
            throw new BizException(ResponseCodeEnum.VERIFICATION_CODE_SEND_FREQUENTLY);
        }

        String verificationCode = RandomUtil.randomNumbers(6);
        log.info("手机号: {}, 已发送验证码: 【{}】", phone, verificationCode);
        
        // 调用三方短信服务
        threadPoolTaskExecutor.submit(() -> {
            String signatureName = "阿里云短信测试";
            String templateCode = "SMS_154950909";
            String templateParam = String.format("{\"code\":\"%s\"}", verificationCode);
            
            aliyunSmsHelper.sendMessage(signatureName, templateCode, phone, templateParam);
        });

        redisTemplate.opsForValue()
                .set(key, verificationCode, 3, TimeUnit.MINUTES);

        return Response.success();
    }

}
