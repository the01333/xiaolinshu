package com.puxinxiaolin.xiaolinshu.kv.biz.controller;

import com.puxinxiaolin.framework.common.response.Response;
import com.puxinxiaolin.xiaolinshu.kv.api.dto.req.AddNoteContentReqDTO;
import com.puxinxiaolin.xiaolinshu.kv.api.dto.req.DeleteNoteContentReqDTO;
import com.puxinxiaolin.xiaolinshu.kv.api.dto.req.FindNoteContentReqDTO;
import com.puxinxiaolin.xiaolinshu.kv.biz.service.NoteContentService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/kv")
@Slf4j
public class NoteContentController {

    @Resource
    private NoteContentService noteContentService;

    /**
     * 添加笔记内容
     *
     * @param request
     * @return
     */
    @PostMapping("/note/content/add")
    public Response<?> addNoteContent(@RequestBody @Validated AddNoteContentReqDTO request) {
        return noteContentService.addNoteContent(request);
    }

    /**
     * 查询笔记内容
     *
     * @param request
     * @return
     */
    @PostMapping("/note/content/find")
    public Response<?> findNoteContent(@RequestBody @Validated FindNoteContentReqDTO request) {
        return noteContentService.findNoteContent(request);
    }

    /**
     * 删除笔记
     *
     * @param request
     * @return
     */
    @PostMapping("/note/content/delete")
    public Response<?> deleteNoteContent(@RequestBody @Validated DeleteNoteContentReqDTO request) {
        return noteContentService.deleteNoteContent(request);
    }

}
