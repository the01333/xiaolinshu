package com.puxinxiaolin.xiaolinshu.note.biz.domain.mapper;

import com.puxinxiaolin.xiaolinshu.note.biz.domain.dataobject.NoteCollectionDO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface NoteCollectionDOMapper {

    /**
     * 取消收藏
     *
     * @param noteCollectionDO
     * @return
     */
    int update2UnCollectByUserIdAndNoteId(NoteCollectionDO noteCollectionDO);

    int insertOrUpdate(NoteCollectionDO noteCollectionDO);

    /**
     * 查询用户最近收藏的笔记
     *
     * @param userId
     * @param limit
     * @return
     */
    List<NoteCollectionDO> selectCollectedByUserIdAndLimit(@Param("userId") Long userId,
                                                           @Param("limit") int limit);

    List<NoteCollectionDO> selectByUserId(Long userId);

    /**
     * 判断笔记是否被收藏
     *
     * @param userId
     * @param noteId
     * @return
     */
    int selectCountByUserIdAndNoteId(@Param("userId") Long userId,
                                     @Param("noteId") Long noteId);

    int deleteByPrimaryKey(Long id);

    int insert(NoteCollectionDO record);

    int insertSelective(NoteCollectionDO record);

    NoteCollectionDO selectByPrimaryKey(Long id);

    int updateByPrimaryKeySelective(NoteCollectionDO record);

    int updateByPrimaryKey(NoteCollectionDO record);
}