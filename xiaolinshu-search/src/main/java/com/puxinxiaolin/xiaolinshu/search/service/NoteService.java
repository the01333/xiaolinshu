package com.puxinxiaolin.xiaolinshu.search.service;

import com.puxinxiaolin.framework.common.response.PageResponse;
import com.puxinxiaolin.xiaolinshu.search.model.vo.SearchNoteReqVO;
import com.puxinxiaolin.xiaolinshu.search.model.vo.SearchNoteRespVO;
import com.puxinxiaolin.xiaolinshu.search.model.vo.SearchUserReqVO;
import com.puxinxiaolin.xiaolinshu.search.model.vo.SearchUserRespVO;

public interface NoteService {
    
    /**
     * 搜索笔记
     *
     * @param request
     * @return
     */
    PageResponse<SearchNoteRespVO> searchNote(SearchNoteReqVO request);

}
