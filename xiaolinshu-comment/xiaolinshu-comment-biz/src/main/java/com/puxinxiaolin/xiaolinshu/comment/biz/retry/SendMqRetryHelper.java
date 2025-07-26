package com.puxinxiaolin.xiaolinshu.comment.biz.retry;

import com.puxinxiaolin.framework.common.util.JsonUtils;
import com.puxinxiaolin.xiaolinshu.comment.biz.model.dto.PublishCommentMqDTO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * @Description: 自定义重试规则 - 重试发送 MQ 消息
 * @Author: YCcLin
 * @Date: 2025/7/26 17:15
 */
@Component
@Slf4j
public class SendMqRetryHelper {
    @Resource
    private RocketMQTemplate rocketMQTemplate;
    @Resource
    private RetryTemplate retryTemplate;
    @Resource(name = "taskExecutor")
    private ThreadPoolTaskExecutor threadPoolTaskExecutor;

    /**
     * 异步发送 MQ
     *
     * @param topic
     * @param body
     */
    public void asyncSend(String topic, String body) {
        log.info("==> 开始异步发送 MQ, Topic: {}, Body: {}", topic, body);

        Message<String> message = MessageBuilder.withPayload(body)
                .build();

        rocketMQTemplate.asyncSend(topic, message, new SendCallback() {
            @Override
            public void onSuccess(SendResult sendResult) {
                log.info("==> 【评论发布】MQ 发送成功，SendResult: {}", sendResult);
            }

            @Override
            public void onException(Throwable throwable) {
                log.error("==> 【评论发布】MQ 发送异常: ", throwable);
                // 有异常就重试
                handleRetry(topic, message);
            }
        });
    }

    /**
     * 重试处理（和原来的注解方式相比交由一个新线程做重试 MQ 发送处理, 而不是在调用线程上执行）
     *
     * @param topic
     * @param message
     */
    private void handleRetry(String topic, Message<String> message) {
        threadPoolTaskExecutor.submit(() -> {
            try {
                retryTemplate.execute((RetryCallback<Void, RuntimeException>) context -> {
                    log.info("==> 开始重试 MQ 发送, 当前重试次数: {}, 时间: {}", context.getRetryCount() + 1, LocalDateTime.now());

                    // 同步发送 MQ
                    rocketMQTemplate.syncSend(topic, message);
                    return null;
                });
            } catch (RuntimeException e) {
                // 所有重试都失败了才会回调
                fallback(e, topic, message.getPayload());
            }
        });
    }

    /**
     * 多次重试失败, 进入兜底方案
     *
     * @param e
     * @param topic
     * @param payload
     */
    private void fallback(RuntimeException e, String topic, String payload) {
        log.error("==> 多次发送失败, 进入兜底方案, Topic: {}, bodyJson: {}", topic, payload);

        // TODO [YCcLin 2025/7/26]: 把失败的消息入库 再自定义一个定时任务去处理
    }


    // ================================= 以下是 Spring Retry 的注解使用方式 ===================================
    /**
     * 因为 MQ 的 send 是同步的, 并且 @Retryable 默认也是在调用线程上执行重试逻辑, 所以可能会导致长时间的阻塞
     * 
     * @param topic
     * @param publishCommentMqDTO
     */
//    @Retryable(
//            retryFor = {Exception.class},    // 需要重试的异常类型
//            maxAttempts = 3,   // 最大重试次数
//            backoff = @Backoff(delay = 1000, multiplier = 2)   // 初始延迟时间 1000ms, 每次重试间隔加倍
//    )
//    public void send(String topic, PublishCommentMqDTO publishCommentMqDTO) {
//        log.info("==> 开始异步发送 MQ, Topic: {}, publishCommentMqDTO: {}", topic, publishCommentMqDTO);
//
//        Message<String> message = MessageBuilder.withPayload(JsonUtils.toJsonString(publishCommentMqDTO))
//                .build();
//
//        rocketMQTemplate.syncSend(topic, message);
//    }
//
//    /**
//     * 兜底方案: 将发送失败的 MQ 写入数据库, 通过定时任务扫表, 将发送失败的 MQ 再次发送, 最终发送成功后, 将该记录物理删除
//     *
//     * @param e
//     * @param topic
//     * @param publishCommentMqDTO
//     */
//    @Recover
//    public void asyncSendMessageFallback(Exception e, String topic, PublishCommentMqDTO publishCommentMqDTO) {
//        log.error("==> 多次发送失败, 进入兜底方案, Topic: {}, publishCommentMqDTO: {}", topic, publishCommentMqDTO);
//        
//        // TODO [YCcLin 2025/7/26]: 把失败的消息入库 再自定义一个定时任务去处理 
//    }

}
