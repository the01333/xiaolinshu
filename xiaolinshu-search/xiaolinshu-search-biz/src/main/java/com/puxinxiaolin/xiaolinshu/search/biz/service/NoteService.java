package com.puxinxiaolin.xiaolinshu.search.biz.service;

import com.puxinxiaolin.framework.common.response.PageResponse;
import com.puxinxiaolin.framework.common.response.Response;
import com.puxinxiaolin.xiaolinshu.search.api.dto.RebuildNoteDocumentReqDTO;
import com.puxinxiaolin.xiaolinshu.search.biz.model.vo.SearchNoteReqVO;
import com.puxinxiaolin.xiaolinshu.search.biz.model.vo.SearchNoteRespVO;

public interface NoteService {

    /**
     * 重建笔记文档
     *
     * @param request
     * @return
     */
    Response<?> rebuildDocument(RebuildNoteDocumentReqDTO request);

    /**
     * 搜索笔记
     *
     * @param request
     * @return
     */
    PageResponse<SearchNoteRespVO> searchNote(SearchNoteReqVO request);

}
