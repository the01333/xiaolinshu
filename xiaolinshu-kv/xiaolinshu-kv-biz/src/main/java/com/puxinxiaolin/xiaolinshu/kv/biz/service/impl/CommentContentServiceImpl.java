package com.puxinxiaolin.xiaolinshu.kv.biz.service.impl;

import com.puxinxiaolin.framework.common.response.Response;
import com.puxinxiaolin.xiaolinshu.kv.api.dto.req.BatchAddCommentContentReqDTO;
import com.puxinxiaolin.xiaolinshu.kv.api.dto.req.CommentContentReqDTO;
import com.puxinxiaolin.xiaolinshu.kv.biz.domain.dataobject.CommentContentDO;
import com.puxinxiaolin.xiaolinshu.kv.biz.domain.dataobject.CommentContentPrimaryKey;
import com.puxinxiaolin.xiaolinshu.kv.biz.service.CommentContentService;
import jakarta.annotation.Resource;
import org.springframework.data.cassandra.core.CassandraTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class CommentContentServiceImpl implements CommentContentService {

    @Resource
    private CassandraTemplate cassandraTemplate;

    /**
     * 批量添加评论内容到 Cassandra
     *
     * @param request
     * @return
     */
    @Override
    public Response<?> batchAddCommentContent(BatchAddCommentContentReqDTO request) {
        List<CommentContentReqDTO> comments = request.getComments();

        List<CommentContentDO> contentDOS = comments.stream()
                .map(comment -> CommentContentDO.builder()
                        .primaryKey(CommentContentPrimaryKey.builder()
                                .noteId(comment.getNoteId())
                                .yearMonth(comment.getYearMonth())
                                .contentId(UUID.fromString(comment.getContentId()))
                                .build())
                        .content(comment.getContent())
                        .build())
                .toList();
        
        cassandraTemplate.batchOps()
                .insert(contentDOS)
                .execute();
        
        return Response.success();
    }

}
