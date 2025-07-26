package com.puxinxiaolin.xiaolinshu.search.controller;

import com.puxinxiaolin.framework.biz.operationlog.aspect.ApiOperationLog;
import com.puxinxiaolin.framework.common.response.PageResponse;
import com.puxinxiaolin.xiaolinshu.search.model.vo.SearchNoteReqVO;
import com.puxinxiaolin.xiaolinshu.search.model.vo.SearchNoteRespVO;
import com.puxinxiaolin.xiaolinshu.search.service.NoteService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/search")
@Slf4j
public class NoteController {
    @Resource
    private NoteService noteService;

    @PostMapping("/note")
    @ApiOperationLog(description = "搜索笔记")
    public PageResponse<SearchNoteRespVO> searchNote(@RequestBody @Validated SearchNoteReqVO request) {
        return noteService.searchNote(request);
    }

}