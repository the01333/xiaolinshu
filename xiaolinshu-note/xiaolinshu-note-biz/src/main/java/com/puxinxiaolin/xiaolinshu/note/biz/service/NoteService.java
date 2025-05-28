package com.puxinxiaolin.xiaolinshu.note.biz.service;

import com.puxinxiaolin.framework.common.response.Response;
import com.puxinxiaolin.xiaolinshu.note.biz.model.vo.PublishNoteReqVO;

public interface NoteService {

    /**
     * 笔记发布
     *
     * @param request
     * @return
     */
    Response<?> publishNote(PublishNoteReqVO request);

}
