package com.puxinxiaolin.xiaolinshu.search.domain.mapper;

import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

public interface SelectMapper {

    /**
     * 查询用户文档所需全字段数据
     *
     * @param userId
     * @return
     */
    List<Map<String, Object>> selectESUserIndexData(@Param("userId") Long userId);

    /**
     * 查询笔记文档所需的全字段数据
     *
     * @param noteId
     * @return
     */
    List<Map<String, Object>> selectESNoteIndexData(@Param("noteId") Long noteId,
                                                    @Param("userId") Long userId);

}
