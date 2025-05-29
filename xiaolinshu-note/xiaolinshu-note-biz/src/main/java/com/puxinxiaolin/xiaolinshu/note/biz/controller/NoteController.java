package com.puxinxiaolin.xiaolinshu.note.biz.controller;

import com.puxinxiaolin.framework.biz.operationlog.aspect.ApiOperationLog;
import com.puxinxiaolin.framework.common.response.Response;
import com.puxinxiaolin.xiaolinshu.note.biz.model.vo.FindNoteDetailReqVO;
import com.puxinxiaolin.xiaolinshu.note.biz.model.vo.FindNoteDetailRspVO;
import com.puxinxiaolin.xiaolinshu.note.biz.model.vo.PublishNoteReqVO;
import com.puxinxiaolin.xiaolinshu.note.biz.model.vo.UpdateNoteReqVO;
import com.puxinxiaolin.xiaolinshu.note.biz.service.NoteService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/note")
@Slf4j
public class NoteController {

    @Resource
    private NoteService noteService;

    @PostMapping(value = "/publish")
    @ApiOperationLog(description = "笔记发布")
    public Response<?> publishNote(@Validated @RequestBody PublishNoteReqVO request) {
        return noteService.publishNote(request);
    }

    @PostMapping(value = "/detail")
    @ApiOperationLog(description = "笔记详情")
    public Response<FindNoteDetailRspVO> findNoteDetail(@Validated @RequestBody FindNoteDetailReqVO request) {
        return noteService.findNoteDetail(request);
    }

    @PostMapping(value = "/update")
    @ApiOperationLog(description = "笔记修改")
    public Response<?> updateNote(@Validated @RequestBody UpdateNoteReqVO request) {
        return noteService.updateNote(request);
    }
    
}
