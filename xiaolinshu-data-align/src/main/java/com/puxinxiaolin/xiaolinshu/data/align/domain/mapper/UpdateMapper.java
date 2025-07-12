package com.puxinxiaolin.xiaolinshu.data.align.domain.mapper;

import org.apache.ibatis.annotations.Param;

public interface UpdateMapper {

    /**
     * 更新 t_user_count 计数表总关注数
     *
     * @param userId
     * @param followingTotal
     * @return
     */
    int updateUserFollowingTotalByUserId(@Param("userId") long userId,
                                         @Param("followingTotal") int followingTotal);

    /**
     * 更新 t_user_count 计数表总粉丝数
     *
     * @param userId
     * @param fansTotal
     * @return
     */
    int updateUserFansTotalByUserId(@Param("userId") long userId,
                                    @Param("fansTotal") int fansTotal);

    /**
     * 更新 t_user_count 计数表获得的总点赞数
     *
     * @param userId
     * @param likeTotal
     * @return
     */
    int updateUserLikeTotalByUserId(@Param("userId") long userId,
                                    @Param("likeTotal") int likeTotal);

    /**
     * 更新 t_user_count 计数表获得的总收藏数
     *
     * @param userId
     * @param collectTotal
     * @return
     */
    int updateUserCollectTotalByUserId(@Param("userId") long userId,
                                       @Param("collectTotal") int collectTotal);

    /**
     * 更新 t_user_count 计数表获得的总笔记发布数
     *
     * @param userId
     * @param noteTotal
     * @return
     */
    int updateUserNoteTotalByUserId(@Param("userId") long userId,
                                    @Param("noteTotal") int noteTotal);

    /**
     * 更新 t_note_count 计数表笔记点赞数
     *
     * @param noteId
     * @param noteLikeTotal
     * @return
     */
    int updateNoteLikeTotalByUserId(@Param("noteId") long noteId,
                                    @Param("noteLikeTotal") int noteLikeTotal);

    /**
     * 更新 t_note_count 计数表笔记收藏数
     *
     * @param noteId
     * @param noteCollectTotal
     * @return
     */
    int updateNoteCollectTotalByUserId(@Param("noteId") long noteId,
                                       @Param("noteCollectTotal") int noteCollectTotal);

}