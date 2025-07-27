package com.puxinxiaolin.xiaolinshu.kv.biz.service;

import com.puxinxiaolin.framework.common.response.Response;
import com.puxinxiaolin.xiaolinshu.kv.api.dto.req.BatchAddCommentContentReqDTO;

public interface CommentContentService {

    /**
     * 批量添加评论内容
     *
     * @param request
     * @return
     */
    Response<?> batchAddCommentContent(BatchAddCommentContentReqDTO request);

}
