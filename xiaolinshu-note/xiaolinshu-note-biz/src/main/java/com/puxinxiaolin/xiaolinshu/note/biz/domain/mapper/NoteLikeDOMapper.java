package com.puxinxiaolin.xiaolinshu.note.biz.domain.mapper;

import com.puxinxiaolin.xiaolinshu.note.biz.domain.dataobject.NoteLikeDO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface NoteLikeDOMapper {
    
    int insertOrUpdate(NoteLikeDO noteLikeDO);
    
    List<NoteLikeDO> selectLikedByUserIdAndLimit(@Param("userId") Long userId,
                                                 @Param("limit")  int limit);
    
    int selectNoteIsLiked(@Param("userId") Long userId,
                          @Param("noteId") Long noteId);
    
    List<NoteLikeDO> selectByUserId(@Param("userId") Long userId);
    
    int selectCountByUserIdAndNoteId(@Param("userId") Long userId,
                                     @Param("noteId") Long noteId);
    
    int deleteByPrimaryKey(Long id);

    int insert(NoteLikeDO record);

    int insertSelective(NoteLikeDO record);

    NoteLikeDO selectByPrimaryKey(Long id);

    int updateByPrimaryKeySelective(NoteLikeDO record);

    int updateByPrimaryKey(NoteLikeDO record);
}