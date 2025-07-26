package com.puxinxiaolin.xiaolinshu.data.align.rpc;

import com.puxinxiaolin.xiaolinshu.search.api.api.SearchFeignApi;
import com.puxinxiaolin.xiaolinshu.search.api.dto.RebuildNoteDocumentReqDTO;
import com.puxinxiaolin.xiaolinshu.search.api.dto.RebuildUserDocumentReqDTO;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

@Component
public class SearchRpcService {
    
    @Resource
    private SearchFeignApi searchFeignApi;
    
    public void rebuildNoteDocument(Long noteId) {
        searchFeignApi.rebuildNoteDocument(RebuildNoteDocumentReqDTO.builder()
                .id(noteId).build());
    }
    
    public void rebuildUserDocument(Long userId) {
        searchFeignApi.rebuildUserDocument(RebuildUserDocumentReqDTO.builder()
                .id(userId).build());
    }
    
}
