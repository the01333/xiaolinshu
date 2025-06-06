package com.puxinxiaolin.xiaolinshu.user.relation.biz.domain.mapper;

import com.puxinxiaolin.xiaolinshu.user.relation.biz.domain.dataobject.FollowingDO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface FollowingDOMapper {

    /**
     * 根据用户 ID 查询其关注列表
     *
     * @param userId
     * @return
     */
    List<FollowingDO> selectAllByUserId(Long userId);

    /**
     * 分页查询
     *
     * @param userId
     * @param offset 偏移量: 从多少开始
     * @param limit  限制条数: 查询的最大条数
     * @return
     */
    List<FollowingDO> selectPageListByUserId(@Param("userId") Long userId,
                                             @Param("offset") long offset,
                                             @Param("limit") long limit);

    long selectCountByUserId(Long userId);

    int deleteByUserIdAndFollowingUserId(@Param("userId") Long userId,
                                         @Param("unfollowUserId") Long unfollowUserId);

    List<FollowingDO> selectByUserId(Long userId);

    int deleteByPrimaryKey(Long id);

    int insert(FollowingDO record);

    int insertSelective(FollowingDO record);

    FollowingDO selectByPrimaryKey(Long id);

    int updateByPrimaryKeySelective(FollowingDO record);

    int updateByPrimaryKey(FollowingDO record);
}