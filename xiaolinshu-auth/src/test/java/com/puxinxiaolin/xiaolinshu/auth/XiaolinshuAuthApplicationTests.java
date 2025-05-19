package com.puxinxiaolin.xiaolinshu.auth;

import com.puxinxiaolin.xiaolinshu.auth.domain.dataobject.UserDO;
import com.puxinxiaolin.xiaolinshu.auth.domain.mapper.UserDOMapper;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class XiaolinshuAuthApplicationTests {
    
    @Resource
    private UserDOMapper userDOMapper;
    
    @Test
    void testSelect() {
        UserDO userDO = userDOMapper.selectByPrimaryKey(1L);
        System.out.println("userDO: " + userDO);
    }
    
}
