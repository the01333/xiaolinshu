package com.puxinxiaolin.xiaolinshu.comment.biz.service;

import com.puxinxiaolin.framework.common.response.PageResponse;
import com.puxinxiaolin.framework.common.response.Response;
import com.puxinxiaolin.xiaolinshu.comment.biz.model.vo.*;

import java.util.List;

public interface CommentService {

    /**
     * 二级评论分页查询
     *
     * @param request
     * @return
     */
    PageResponse<FindChildCommentPageListRspVO> findChildCommentPageList(FindChildCommentPageListReqVO request);

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
