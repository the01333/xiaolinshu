package com.puxinxiaolin.xiaolinshu.note.biz.domain.mapper;

import com.puxinxiaolin.xiaolinshu.note.biz.domain.dataobject.NoteDO;

public interface NoteDOMapper {

    /**
     * 查询笔记发布者的 ID
     *
     * @param noteId
     * @return
     */
    Long selectCreatorIdByNoteId(Long noteId);

    int selectCountByNoteId(Long id);

    int updateIsTop(NoteDO noteDO);

    int updateVisibleOnlyMe(NoteDO noteDO);

    int deleteByPrimaryKey(Long id);

    int insert(NoteDO record);

    int insertSelective(NoteDO record);

    NoteDO selectByPrimaryKey(Long id);

    int updateByPrimaryKeySelective(NoteDO record);

    int updateByPrimaryKey(NoteDO record);
}