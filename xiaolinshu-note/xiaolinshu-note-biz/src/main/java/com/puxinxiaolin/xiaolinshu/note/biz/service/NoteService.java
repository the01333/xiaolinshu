package com.puxinxiaolin.xiaolinshu.note.biz.service;

import com.puxinxiaolin.framework.common.response.Response;
import com.puxinxiaolin.xiaolinshu.note.biz.model.vo.*;

public interface NoteService {

    /**
     * 笔记发布
     *
     * @param request
     * @return
     */
    Response<?> publishNote(PublishNoteReqVO request);

    /**
     * 查询笔记详情
     *
     * @param request
     * @return
     */
    Response<FindNoteDetailRspVO> findNoteDetail(FindNoteDetailReqVO request);

    /**
     * 更新笔记
     *
     * @param request
     * @return
     */
    Response<?> updateNote(UpdateNoteReqVO request);

    /**
     * 删除本地笔记缓存
     *
     * @param noteId
     */
    void deleteNoteLocalCache(Long noteId);

    /**
     * 删除笔记
     *
     * @param request
     * @return
     */
    Response<?> deleteNote(DeleteNoteReqVO request);

    /**
     * 笔记仅对自己可见
     *
     * @param request
     * @return
     */
    Response<?> visibleOnlyMe(UpdateNoteVisibleOnlyMeReqVO request);

    /**
     * 笔记置顶 / 取消置顶
     *
     * @param request
     * @return
     */
    Response<?> topNote(TopNoteReqVO request);

    /**
     * 点赞笔记
     *
     * @param request
     * @return
     */
    Response<?> likeNote(LikeNoteReqVO request);

    /**
     * 取消点赞笔记
     *
     * @param request
     * @return
     */
    Response<?> unlikeNote(UnlikeNoteReqVO request);

}
