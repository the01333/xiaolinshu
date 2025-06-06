package com.puxinxiaolin.xiaolinshu.user.biz.domain.mapper;

import com.puxinxiaolin.xiaolinshu.user.biz.domain.dataobject.UserDO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface UserDOMapper {

    /**
     * 批量查询用户信息
     *
     * @param ids
     * @return
     */
    List<UserDO> selectByIds(@Param("ids") List<Long> ids);

    /**
     * 根据手机号查询用户信息
     *
     * @param phone
     * @return
     */
    UserDO selectByPhone(String phone);

    int deleteByPrimaryKey(Long id);

    int insert(UserDO record);

    int insertSelective(UserDO record);

    UserDO selectByPrimaryKey(Long id);

    int updateByPrimaryKeySelective(UserDO record);

    int updateByPrimaryKey(UserDO record);
}