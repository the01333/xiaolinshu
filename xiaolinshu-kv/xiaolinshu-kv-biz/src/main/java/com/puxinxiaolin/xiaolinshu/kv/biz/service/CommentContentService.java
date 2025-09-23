package com.puxinxiaolin.xiaolinshu.kv.biz.service;

import com.puxinxiaolin.framework.common.response.Response;
import com.puxinxiaolin.xiaolinshu.kv.api.dto.req.BatchAddCommentContentReqDTO;
import com.puxinxiaolin.xiaolinshu.kv.api.dto.req.BatchFindCommentContentReqDTO;
import com.puxinxiaolin.xiaolinshu.kv.api.dto.req.DeleteCommentContentReqDTO;

public interface CommentContentService {
    
    /**
     * 批量查询评论内容
     *
     * @param request
     * @return
     */
    Response<?> batchFindCommentContent(BatchFindCommentContentReqDTO request);

    /**
     * 批量添加评论内容
     *
     * @param request
     * @return
     */
    Response<?> batchAddCommentContent(BatchAddCommentContentReqDTO request);

    /**
     * 删除评论内容
     *
     * @param request
     * @return
     */
    Response<?> deleteCommentContent(DeleteCommentContentReqDTO request);

}
