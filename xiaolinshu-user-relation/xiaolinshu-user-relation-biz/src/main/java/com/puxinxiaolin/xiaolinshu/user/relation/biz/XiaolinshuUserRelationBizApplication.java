package com.puxinxiaolin.xiaolinshu.user.relation.biz;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.puxinxiaolin.xiaolinshu.user.relation.biz.domain.mapper")
public class XiaolinshuUserRelationBizApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(XiaolinshuUserRelationBizApplication.class, args);
    }
    
}
