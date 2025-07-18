package com.puxinxiaolin.xiaolinshu.search.service;

import com.puxinxiaolin.framework.common.response.PageResponse;
import com.puxinxiaolin.xiaolinshu.search.model.vo.SearchUserReqVO;
import com.puxinxiaolin.xiaolinshu.search.model.vo.SearchUserRespVO;

public interface UserService {

    /**
     * 搜索用户
     *
     * @param request
     * @return
     */
    PageResponse<SearchUserRespVO> searchUser(SearchUserReqVO request);

}
