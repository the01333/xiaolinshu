package com.puxinxiaolin.xiaolinshu.data.align.domain.mapper;

import org.apache.ibatis.annotations.Param;

public interface InsertRecordMapper {

    /**
     * 用户关注数: 计数变更
     *
     * @param tableNameSuffix
     * @param userId
     */
    void insert2DataAlignUserFollowingCountTempTable(@Param("tableNameSuffix") String tableNameSuffix,
                                                     @Param("userId") Long userId);

    /**
     * 用户粉丝数: 计数变更
     *
     * @param tableNameSuffix
     * @param userId
     */
    void insert2DataAlignUserFansCountTempTable(@Param("tableNameSuffix") String tableNameSuffix,
                                                @Param("userId") Long userId);

    /**
     * 用户已发布笔记数: 计数变更
     *
     * @param tableNameSuffix
     * @param userId
     */
    void insert2DataAlignUserNotePublishCountTempTable(@Param("tableNameSuffix") String tableNameSuffix,
                                                       @Param("userId") Long userId);

    /**
     * 笔记收藏数: 计数变更
     *
     * @param tableNameSuffix
     * @param noteId
     */
    void insert2DataAlignNoteCollectCountTempTable(@Param("tableNameSuffix") String tableNameSuffix,
                                                   @Param("noteId") Long noteId);

    /**
     * 用户获得的收藏数: 计数变更
     *
     * @param tableNameSuffix
     * @param userId
     */
    void insert2DataAlignUserCollectCountTempTable(@Param("tableNameSuffix") String tableNameSuffix,
                                                   @Param("userId") Long userId);

    /**
     * 笔记点赞数: 计数变更
     *
     * @param tableNameSuffix
     * @param noteId
     */
    void insert2DataAlignNoteLikeCountTempTable(@Param("tableNameSuffix") String tableNameSuffix,
                                                @Param("noteId") Long noteId);

    /**
     * 用户获得的点赞数: 计数变更
     *
     * @param tableNameSuffix
     * @param userId
     */
    void insert2DataAlignUserLikeCountTempTable(@Param("tableNameSuffix") String tableNameSuffix,
                                                @Param("userId") Long userId);

}
