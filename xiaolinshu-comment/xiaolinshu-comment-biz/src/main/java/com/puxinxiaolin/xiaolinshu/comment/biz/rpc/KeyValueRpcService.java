package com.puxinxiaolin.xiaolinshu.comment.biz.rpc;

import com.google.common.collect.Lists;
import com.puxinxiaolin.framework.common.constant.DateConstants;
import com.puxinxiaolin.framework.common.response.Response;
import com.puxinxiaolin.xiaolinshu.comment.biz.model.bo.CommentBO;
import com.puxinxiaolin.xiaolinshu.kv.api.api.KeyValueFeignApi;
import com.puxinxiaolin.xiaolinshu.kv.api.dto.req.BatchAddCommentContentReqDTO;
import com.puxinxiaolin.xiaolinshu.kv.api.dto.req.CommentContentReqDTO;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class KeyValueRpcService {

    @Resource
    private KeyValueFeignApi keyValueFeignApi;

    /**
     * 批量存储批量内容到 Cassandra
     *
     * @param commentBOS
     * @return
     */
    public boolean batchSaveCommentContent(List<CommentBO> commentBOS) {
        List<CommentContentReqDTO> result = Lists.newArrayList();
        
        commentBOS.forEach(bo -> {
            CommentContentReqDTO dto = CommentContentReqDTO.builder()
                    .noteId(bo.getNoteId())
                    .content(bo.getContent())
                    .contentId(bo.getContentUuid())
                    .yearMonth(bo.getCreateTime().format(DateConstants.Y_M))
                    .build();
            
            result.add(dto);
        });

        BatchAddCommentContentReqDTO reqDTO = BatchAddCommentContentReqDTO.builder()
                .comments(result)
                .build();
        Response<?> response = keyValueFeignApi.batchAddCommentContent(reqDTO);
        if (!response.isSuccess()) {
            throw new RuntimeException("批量存储评论内容失败");
        }
        
        return true;
    }

}
