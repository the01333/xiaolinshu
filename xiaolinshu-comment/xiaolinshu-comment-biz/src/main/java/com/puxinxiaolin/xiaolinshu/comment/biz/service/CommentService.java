package com.puxinxiaolin.xiaolinshu.comment.biz.service;

import com.puxinxiaolin.framework.common.response.Response;
import com.puxinxiaolin.xiaolinshu.comment.biz.model.vo.PublishCommentReqVO;

public interface CommentService {

    /**
     * 发布评论
     *
     * @param request
     * @return
     */
    Response<?> publishComment(PublishCommentReqVO request);

}
