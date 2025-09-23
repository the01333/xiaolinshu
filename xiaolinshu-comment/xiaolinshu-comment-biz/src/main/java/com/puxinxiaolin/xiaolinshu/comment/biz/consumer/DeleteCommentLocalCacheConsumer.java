package com.puxinxiaolin.xiaolinshu.comment.biz.consumer;

import com.puxinxiaolin.xiaolinshu.comment.biz.constant.MQConstants;
import com.puxinxiaolin.xiaolinshu.comment.biz.service.CommentService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.MessageModel;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

/**
 * @Description: 删除目标评论的本地缓存
 * @Author: YCcLin
 * @Date: 2025/9/23 23:06
 */
@Component
@Slf4j
@RocketMQMessageListener(
        consumerGroup = "xiaolinshu_group_" + MQConstants.TOPIC_DELETE_COMMENT_LOCAL_CACHE,
        topic = MQConstants.TOPIC_DELETE_COMMENT_LOCAL_CACHE,
        messageModel = MessageModel.BROADCASTING
)
public class DeleteCommentLocalCacheConsumer implements RocketMQListener<String> {

    @Resource
    private CommentService commentService;

    @Override
    public void onMessage(String message) {
        Long commentId = Long.valueOf(message);
        log.info("## 消费者消费成功, commentId: {}", commentId);

        commentService.deleteCommentLocalCache(commentId);
    }

}
