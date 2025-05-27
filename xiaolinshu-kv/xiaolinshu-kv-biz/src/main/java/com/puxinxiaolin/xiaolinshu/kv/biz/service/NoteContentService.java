package com.puxinxiaolin.xiaolinshu.kv.biz.service;

import com.puxinxiaolin.framework.common.response.Response;
import com.puxinxiaolin.xiaolinshu.kv.api.dto.req.AddNoteContentReqDTO;
import com.puxinxiaolin.xiaolinshu.kv.api.dto.req.DeleteNoteContentReqDTO;
import com.puxinxiaolin.xiaolinshu.kv.api.dto.req.FindNoteContentReqDTO;
import com.puxinxiaolin.xiaolinshu.kv.api.dto.rsp.FindNoteContentRspDTO;

public interface NoteContentService {

    /**
     * 添加笔记内容
     *
     * @param request
     * @return
     */
    Response<?> addNoteContent(AddNoteContentReqDTO request);

    /**
     * 查询笔记内容
     *
     * @param request
     * @return
     */
    Response<FindNoteContentRspDTO> findNoteContent(FindNoteContentReqDTO request);

    /**
     * 删除笔记内容
     *
     * @param request
     * @return
     */
    Response<?> deleteNoteContent(DeleteNoteContentReqDTO request);
    
}
