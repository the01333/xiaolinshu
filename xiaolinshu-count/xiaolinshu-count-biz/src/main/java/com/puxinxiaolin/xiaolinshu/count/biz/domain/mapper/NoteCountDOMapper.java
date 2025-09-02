package com.puxinxiaolin.xiaolinshu.count.biz.domain.mapper;

import com.puxinxiaolin.xiaolinshu.count.biz.domain.dataobject.NoteCountDO;
import org.apache.ibatis.annotations.Param;

public interface NoteCountDOMapper {

    /**
     * 添加记录或更新笔记评论数
     *
     * @param count
     * @param noteId
     * @return
     */
    int insertOrUpdateCommentTotalByNoteId(@Param("count") int count,
                                           @Param("noteId") Long noteId);

    /**
     * 新增或修改收藏数
     *
     * @param count
     * @param noteId
     * @return
     */
    int insertOrUpdateCollectTotalByNoteId(@Param("count") Integer count,
                                           @Param("noteId") Long noteId);

    /**
     * 新增或修改点赞数
     *
     * @param count
     * @param noteId
     * @return
     */
    int insertOrUpdateLikeTotalByNoteId(@Param("count") Integer count,
                                        @Param("noteId") Long noteId);

    int deleteByPrimaryKey(Long id);

    int insert(NoteCountDO record);

    int insertSelective(NoteCountDO record);

    NoteCountDO selectByPrimaryKey(Long id);

    int updateByPrimaryKeySelective(NoteCountDO record);

    int updateByPrimaryKey(NoteCountDO record);
}