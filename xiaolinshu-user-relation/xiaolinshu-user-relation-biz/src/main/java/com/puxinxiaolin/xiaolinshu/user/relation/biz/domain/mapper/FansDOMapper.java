package com.puxinxiaolin.xiaolinshu.user.relation.biz.domain.mapper;

import com.puxinxiaolin.xiaolinshu.user.relation.biz.domain.dataobject.FansDO;
import org.apache.ibatis.annotations.Param;

public interface FansDOMapper {

    int deleteByUserIdAndFansUserId(@Param("userId") Long userId,
                                    @Param("fansUserId") Long fansUserId);
    
    int deleteByPrimaryKey(Long id);

    int insert(FansDO record);

    int insertSelective(FansDO record);

    FansDO selectByPrimaryKey(Long id);

    int updateByPrimaryKeySelective(FansDO record);

    int updateByPrimaryKey(FansDO record);
}