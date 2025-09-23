package com.puxinxiaolin.xiaolinshu.comment.biz.rpc;

import cn.hutool.core.collection.CollUtil;
import com.google.common.collect.Lists;
import com.puxinxiaolin.framework.common.constant.DateConstants;
import com.puxinxiaolin.framework.common.response.Response;
import com.puxinxiaolin.xiaolinshu.comment.biz.model.bo.CommentBO;
import com.puxinxiaolin.xiaolinshu.kv.api.api.KeyValueFeignApi;
import com.puxinxiaolin.xiaolinshu.kv.api.dto.req.*;
import com.puxinxiaolin.xiaolinshu.kv.api.dto.rsp.FindCommentContentRspDTO;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

@Component
public class KeyValueRpcService {

    @Resource
    private KeyValueFeignApi keyValueFeignApi;

    /**
     * 删除评论内容
     *
     * @param noteId
     * @param createTime
     * @param contentId
     * @return
     */
    public boolean deleteCommentContent(Long noteId, LocalDateTime createTime, String contentId) {
        DeleteCommentContentReqDTO dto = DeleteCommentContentReqDTO.builder()
                .contentId(contentId)
                .yearMonth(DateConstants.Y_M.format(createTime))
                .noteId(noteId)
                .build();

        Response<?> response = keyValueFeignApi.deleteCommentContent(dto);
        if (!response.isSuccess()) {
            throw new RuntimeException("删除评论内容失败");
        }

        return true;
    }

    /**
     * 批量查询评论内容
     *
     * @param noteId
     * @param findCommentContentReqDTOS
     * @return
     */
    public List<FindCommentContentRspDTO> batchFindCommentContent(Long noteId, List<FindCommentContentReqDTO> findCommentContentReqDTOS) {
        BatchFindCommentContentReqDTO dto = BatchFindCommentContentReqDTO.builder()
                .noteId(noteId)
                .commentContentKeys(findCommentContentReqDTOS)
                .build();

        Response<List<FindCommentContentRspDTO>> response = keyValueFeignApi.batchFindCommentContent(dto);
        if (!response.isSuccess() || Objects.isNull(response.getData())
                || CollUtil.isEmpty(response.getData())) {
            return null;
        }

        return response.getData();
    }

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
