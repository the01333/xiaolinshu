package com.puxinxiaolin.xiaolinshu.search.api.api;

import com.puxinxiaolin.framework.common.response.Response;
import com.puxinxiaolin.xiaolinshu.search.api.constant.ApiConstants;
import com.puxinxiaolin.xiaolinshu.search.api.dto.RebuildNoteDocumentReqDTO;
import com.puxinxiaolin.xiaolinshu.search.api.dto.RebuildUserDocumentReqDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = ApiConstants.SERVICE_NAME)
public interface SearchFeignApi {

    String PREFIX = "/search";
    
    @PostMapping(PREFIX + "/note/document/rebuild")
    Response<?> rebuildNoteDocument(@RequestBody RebuildNoteDocumentReqDTO request);

    @PostMapping(PREFIX + "/user/document/rebuild")
    Response<?> rebuildUserDocument(@RequestBody RebuildUserDocumentReqDTO request);
}
