package com.puxinxiaolin.xiaolinshu.user.relation.biz.domain.mapper;

import com.puxinxiaolin.xiaolinshu.user.relation.biz.domain.dataobject.FansDO;
import org.apache.ibatis.annotations.Param;

public interface FansDOMapper {

    /**
     * 根据用户 ID 查询粉丝数量
     *
     * @param userId
     * @return
     */
    int selectCountByUserId(Long userId);

    int deleteByUserIdAndFansUserId(@Param("userId") Long userId,
                                    @Param("fansUserId") Long fansUserId);

    int deleteByPrimaryKey(Long id);

    int insert(FansDO record);

    int insertSelective(FansDO record);

    FansDO selectByPrimaryKey(Long id);

    int updateByPrimaryKeySelective(FansDO record);

    int updateByPrimaryKey(FansDO record);
}