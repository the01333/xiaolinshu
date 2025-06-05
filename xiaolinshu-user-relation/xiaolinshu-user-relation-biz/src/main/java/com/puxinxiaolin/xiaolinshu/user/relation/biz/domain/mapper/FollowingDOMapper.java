package com.puxinxiaolin.xiaolinshu.user.relation.biz.domain.mapper;

import com.puxinxiaolin.xiaolinshu.user.relation.biz.domain.dataobject.FollowingDO;

import java.util.List;

public interface FollowingDOMapper {
    
    List<FollowingDO> selectByUserId(Long userId);
    
    int deleteByPrimaryKey(Long id);

    int insert(FollowingDO record);

    int insertSelective(FollowingDO record);

    FollowingDO selectByPrimaryKey(Long id);

    int updateByPrimaryKeySelective(FollowingDO record);

    int updateByPrimaryKey(FollowingDO record);
}