package com.puxinxiaolin.xiaolinshu.user.relation.biz;

import com.puxinxiaolin.framework.common.util.JsonUtils;
import com.puxinxiaolin.xiaolinshu.user.relation.biz.constant.MqConstants;
import com.puxinxiaolin.xiaolinshu.user.relation.biz.enums.FollowUnfollowTypeEnum;
import com.puxinxiaolin.xiaolinshu.user.relation.biz.model.dto.CountFollowUnfollowMqDTO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.messaging.support.MessageBuilder;

@SpringBootTest
@Slf4j
public class MQTests {

    @Resource
    private RocketMQTemplate rocketMQTemplate;

    /**
     * 测试：发送计数 MQ, 以统计粉丝数
     */
    @Test
    void testSendCountFollowUnfollowMQ() {
        // 循环发送 3200 条 MQ
        for (long i = 0; i < 3200; i++) {
            CountFollowUnfollowMqDTO mqDTO = CountFollowUnfollowMqDTO.builder()
                    .userId(i + 1)
                    .targetUserId(27L)
                    .type(FollowUnfollowTypeEnum.FOLLOW.getCode())
                    .build();

            org.springframework.messaging.Message<String> message = MessageBuilder.withPayload(JsonUtils.toJsonString(mqDTO))
                    .build();

            rocketMQTemplate.asyncSend(MqConstants.TOPIC_COUNT_FANS, message, new SendCallback() {
                @Override
                public void onSuccess(SendResult sendResult) {
                    log.info("==> 【计数服务：粉丝数】MQ 发送成功，SendResult: {}", sendResult);
                }

                @Override
                public void onException(Throwable throwable) {
                    log.error("==> 【计数服务：粉丝数】MQ 发送异常: ", throwable);
                }
            });
        }

    }


}
