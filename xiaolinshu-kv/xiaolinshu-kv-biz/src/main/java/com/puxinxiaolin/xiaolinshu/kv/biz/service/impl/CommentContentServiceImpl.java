package com.puxinxiaolin.xiaolinshu.kv.biz.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.google.common.collect.Lists;
import com.puxinxiaolin.framework.common.response.Response;
import com.puxinxiaolin.xiaolinshu.kv.api.dto.req.BatchAddCommentContentReqDTO;
import com.puxinxiaolin.xiaolinshu.kv.api.dto.req.BatchFindCommentContentReqDTO;
import com.puxinxiaolin.xiaolinshu.kv.api.dto.req.CommentContentReqDTO;
import com.puxinxiaolin.xiaolinshu.kv.api.dto.req.FindCommentContentReqDTO;
import com.puxinxiaolin.xiaolinshu.kv.api.dto.rsp.FindCommentContentRspDTO;
import com.puxinxiaolin.xiaolinshu.kv.biz.domain.dataobject.CommentContentDO;
import com.puxinxiaolin.xiaolinshu.kv.biz.domain.dataobject.CommentContentPrimaryKey;
import com.puxinxiaolin.xiaolinshu.kv.biz.domain.repository.CommentContentRepository;
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
    @Resource
    private CommentContentRepository commentContentRepository;

    /**
     * 批量查询评论内容
     *
     * @param request
     * @return
     */
    @Override
    public Response<?> batchFindCommentContent(BatchFindCommentContentReqDTO request) {
        Long noteId = request.getNoteId();
        List<FindCommentContentReqDTO> commentContentKeys = request.getCommentContentKeys();
        
        // 过滤出年月和内容 UUID
        List<String> yearMonths = commentContentKeys.stream()
                .map(FindCommentContentReqDTO::getYearMonth)
                .distinct()
                .toList();
        List<UUID> contentIds = commentContentKeys.stream()
                .map(key -> UUID.fromString(key.getContentId()))
                .distinct()
                .toList();

        List<CommentContentDO> commentContentDOS = commentContentRepository.findByPKNoteIdAndPKYearMonthAndPKContentId(noteId, yearMonths, contentIds);

        // DO -> rspDTO
        List<FindCommentContentRspDTO> rspDTO = Lists.newArrayList();
        if (CollUtil.isNotEmpty(commentContentDOS)) {
            rspDTO = commentContentDOS.stream()
                    .map(commentContentDO -> FindCommentContentRspDTO.builder()
                                .contentId(commentContentDO.getPrimaryKey().getContentId().toString())
                                .content(commentContentDO.getContent())
                                .build()
                    ).toList();
        }
        
        return Response.success(rspDTO);
    }

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
