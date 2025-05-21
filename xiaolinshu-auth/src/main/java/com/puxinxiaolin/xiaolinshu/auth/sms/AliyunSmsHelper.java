package com.puxinxiaolin.xiaolinshu.auth.sms;

import com.aliyun.dysmsapi20170525.Client;
import com.aliyun.dysmsapi20170525.models.SendSmsRequest;
import com.aliyun.dysmsapi20170525.models.SendSmsResponse;
import com.aliyun.teautil.models.RuntimeOptions;
import com.puxinxiaolin.framework.common.util.JsonUtils;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class AliyunSmsHelper {

    @Resource
    private Client client;

    /**
     * 发送短信
     *
     * @param signatureName
     * @param templateCode
     * @param phone
     * @param templateParam
     * @return
     */
    public boolean sendMessage(String signatureName, String templateCode, String phone, String templateParam) {
        SendSmsRequest sendSmsRequest = new SendSmsRequest()
                .setSignName(signatureName)
                .setTemplateCode(templateCode)
                .setPhoneNumbers(phone)
                .setTemplateParam(templateParam);
        RuntimeOptions runtimeOptions = new RuntimeOptions();

        try {
            log.info("==> 开始发送短信, phone: {}, signName: {}, templateCode: {}, templateParam: {}",
                    phone, signatureName, templateCode, templateParam);

            SendSmsResponse response = client.sendSmsWithOptions(sendSmsRequest, runtimeOptions);
            
            log.info("==> 发送短信成功, response: {}", JsonUtils.toJsonString(response));
            return true;
        } catch (Exception e) {
            log.error("==> 发送短信失败: {}", e.getMessage(), e);
            return false;
        }
    }

}
