package com.puxinxiaolin.xiaolinshu.count.biz.domain.mapper;

import com.puxinxiaolin.xiaolinshu.count.biz.domain.dataobject.UserCountDO;
import org.apache.ibatis.annotations.Param;

public interface UserCountDOMapper {

    /**
     * 添加或更新笔记点赞总数
     *
     * @param count
     * @param userId
     * @return
     */
    int insertOrUpdateLikeTotalByUserId(@Param("count") Integer count,
                                        @Param("userId") Long userId);

    /**
     * 添加或更新关注总数
     *
     * @param count
     * @param userId
     * @return
     */
    int insertOrUpdateFollowingTotalByUserId(@Param("count") Integer count,
                                             @Param("userId") Long userId);

    /**
     * 添加或更新粉丝总数
     *
     * @param count
     * @param userId
     * @return
     */
    int insertOrUpdateFansTotalByUserId(@Param("count") Integer count,
                                        @Param("userId") Long userId);

    int deleteByPrimaryKey(Long id);

    int insert(UserCountDO record);

    int insertSelective(UserCountDO record);

    UserCountDO selectByPrimaryKey(Long id);

    int updateByPrimaryKeySelective(UserCountDO record);

    int updateByPrimaryKey(UserCountDO record);
}