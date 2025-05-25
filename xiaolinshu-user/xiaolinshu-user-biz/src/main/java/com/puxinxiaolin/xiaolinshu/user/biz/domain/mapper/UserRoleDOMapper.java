package com.puxinxiaolin.xiaolinshu.user.biz.domain.mapper;

import com.puxinxiaolin.xiaolinshu.user.biz.domain.dataobject.UserRoleDO;

public interface UserRoleDOMapper {
    
    int deleteByPrimaryKey(Long id);

    int insert(UserRoleDO record);

    int insertSelective(UserRoleDO record);

    UserRoleDO selectByPrimaryKey(Long id);

    int updateByPrimaryKeySelective(UserRoleDO record);

    int updateByPrimaryKey(UserRoleDO record);
}