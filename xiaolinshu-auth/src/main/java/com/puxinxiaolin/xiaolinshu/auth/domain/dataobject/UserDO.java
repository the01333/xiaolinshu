package com.puxinxiaolin.xiaolinshu.auth.domain.dataobject;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Setter
@Getter
public class UserDO {
    
    private Long id;

    private String username;

    private Date createTime;

    private Date updateTime;

}