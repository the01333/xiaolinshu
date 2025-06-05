package com.puxinxiaolin.xiaolinshu.user.relation.biz;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@MapperScan("com.puxinxiaolin.xiaolinshu.user.relation.biz.domain.mapper")
@EnableFeignClients(basePackages = "com.puxinxiaolin.xiaolinshu")
public class XiaolinshuUserRelationBizApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(XiaolinshuUserRelationBizApplication.class, args);
    }
    
}
