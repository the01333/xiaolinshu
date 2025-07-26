package com.puxinxiaolin.xiaolinshu.search.biz.service;

import com.puxinxiaolin.framework.common.response.PageResponse;
import com.puxinxiaolin.framework.common.response.Response;
import com.puxinxiaolin.xiaolinshu.search.api.dto.RebuildUserDocumentReqDTO;
import com.puxinxiaolin.xiaolinshu.search.biz.model.vo.SearchUserReqVO;
import com.puxinxiaolin.xiaolinshu.search.biz.model.vo.SearchUserRespVO;

public interface UserService {

    /**
     * 重建用户文档
     *
     * @param request
     * @return
     */
    Response<?> rebuildDocument(RebuildUserDocumentReqDTO request);

    /**
     * 搜索用户
     *
     * @param request
     * @return
     */
    PageResponse<SearchUserRespVO> searchUser(SearchUserReqVO request);

}
