package com.puxinxiaolin.xiaolinshu.comment.biz.service;

import com.puxinxiaolin.framework.common.response.PageResponse;
import com.puxinxiaolin.framework.common.response.Response;
import com.puxinxiaolin.xiaolinshu.comment.biz.model.vo.FindCommentItemRspVO;
import com.puxinxiaolin.xiaolinshu.comment.biz.model.vo.FindCommentPageListReqVO;
import com.puxinxiaolin.xiaolinshu.comment.biz.model.vo.PublishCommentReqVO;

public interface CommentService {

    /**
     * 评论列表分页查询
     *
     * @param request
     * @return
     */
    PageResponse<FindCommentItemRspVO> findCommentPageList(FindCommentPageListReqVO request);

    /**
     * 发布评论
     *
     * @param request
     * @return
     */
    Response<?> publishComment(PublishCommentReqVO request);

}
