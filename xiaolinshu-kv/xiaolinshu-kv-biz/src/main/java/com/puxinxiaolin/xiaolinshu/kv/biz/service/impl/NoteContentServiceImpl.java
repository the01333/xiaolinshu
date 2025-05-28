package com.puxinxiaolin.xiaolinshu.kv.biz.service.impl;

import com.puxinxiaolin.framework.common.exception.BizException;
import com.puxinxiaolin.framework.common.response.Response;
import com.puxinxiaolin.xiaolinshu.kv.api.dto.req.AddNoteContentReqDTO;
import com.puxinxiaolin.xiaolinshu.kv.api.dto.req.DeleteNoteContentReqDTO;
import com.puxinxiaolin.xiaolinshu.kv.api.dto.req.FindNoteContentReqDTO;
import com.puxinxiaolin.xiaolinshu.kv.api.dto.rsp.FindNoteContentRspDTO;
import com.puxinxiaolin.xiaolinshu.kv.biz.domain.dataobject.NoteContentDO;
import com.puxinxiaolin.xiaolinshu.kv.biz.domain.repository.NoteContentRepository;
import com.puxinxiaolin.xiaolinshu.kv.biz.enums.ResponseCodeEnum;
import com.puxinxiaolin.xiaolinshu.kv.biz.service.NoteContentService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
public class NoteContentServiceImpl implements NoteContentService {

    @Resource
    private NoteContentRepository noteContentRepository;

    /**
     * 添加笔记内容
     *
     * @param request
     * @return
     */
    @Override
    public Response<?> addNoteContent(AddNoteContentReqDTO request) {
        String uuid = request.getUuid();
        String content = request.getContent();

        NoteContentDO noteContentDO = NoteContentDO.builder()
                .id(UUID.fromString(uuid))   // TODO [YCcLin 2025/5/26]: 后续从笔记服务获取笔记 ID 
                .content(content)
                .build();

        noteContentRepository.save(noteContentDO);
        return Response.success();
    }

    /**
     * 查询笔记内容
     *
     * @param request
     * @return
     */
    @Override
    public Response<FindNoteContentRspDTO> findNoteContent(FindNoteContentReqDTO request) {
        String uuid = request.getUuid();

        Optional<NoteContentDO> optional = noteContentRepository.findById(UUID.fromString(uuid));
        if (optional.isEmpty()) {
            throw new BizException(ResponseCodeEnum.NOTE_CONTENT_NOT_FOUND);
        }

        NoteContentDO noteContentDO = optional.get();
        FindNoteContentRspDTO response = FindNoteContentRspDTO.builder()
                .uuid(noteContentDO.getId())
                .content(noteContentDO.getContent())
                .build();

        return Response.success(response);
    }

    /**
     * 删除笔记内容
     *
     * @param request
     * @return
     */
    @Override
    public Response<?> deleteNoteContent(DeleteNoteContentReqDTO request) {
        String uuid = request.getUuid();
        noteContentRepository.deleteById(UUID.fromString(uuid));

        return Response.success();
    }

}
