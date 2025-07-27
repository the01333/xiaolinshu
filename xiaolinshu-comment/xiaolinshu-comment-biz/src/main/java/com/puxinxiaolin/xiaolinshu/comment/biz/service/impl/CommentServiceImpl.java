package com.puxinxiaolin.xiaolinshu.comment.biz.service.impl;

import com.google.common.base.Preconditions;
import com.puxinxiaolin.framework.biz.context.holder.LoginUserContextHolder;
import com.puxinxiaolin.framework.common.response.Response;
import com.puxinxiaolin.framework.common.util.JsonUtils;
import com.puxinxiaolin.xiaolinshu.comment.biz.constant.MQConstants;
import com.puxinxiaolin.xiaolinshu.comment.biz.model.dto.PublishCommentMqDTO;
import com.puxinxiaolin.xiaolinshu.comment.biz.model.vo.PublishCommentReqVO;
import com.puxinxiaolin.xiaolinshu.comment.biz.retry.SendMqRetryHelper;
import com.puxinxiaolin.xiaolinshu.comment.biz.rpc.DistributedIdGeneratorRpcService;
import com.puxinxiaolin.xiaolinshu.comment.biz.service.CommentService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@Slf4j
public class CommentServiceImpl implements CommentService {
    @Resource
    private SendMqRetryHelper sendMqRetryHelper;
    @Resource
    private DistributedIdGeneratorRpcService distributedIdGeneratorRpcService;

    /**
     * 发布评论（由于该接口是高并发接口, 这里发 MQ 来实现异步写）
     *
     * @param request
     * @return
     */
    @Override
    public Response<?> publishComment(PublishCommentReqVO request) {
        String content = request.getContent();
        String imageUrl = request.getImageUrl();

        Preconditions.checkArgument(StringUtils.isNotBlank(content) || StringUtils.isNotBlank(imageUrl),
                "评论正文和图片不能同时为空");

        Long creatorId = LoginUserContextHolder.getUserId();
        String commentId = distributedIdGeneratorRpcService.generateCommentId();
        PublishCommentMqDTO mqDTO = PublishCommentMqDTO.builder()
                .noteId(request.getNoteId())
                .content(content)
                .imageUrl(imageUrl)
                .commentId(Long.valueOf(commentId))
                .replyCommentId(request.getReplyCommentId())
                .createTime(LocalDateTime.now())
                .creatorId(creatorId)
                .build();
        
        // 走 MQ（包括重试机制）
        sendMqRetryHelper.asyncSend(MQConstants.TOPIC_PUBLISH_COMMENT, JsonUtils.toJsonString(mqDTO));

        return Response.success();
    }

}
