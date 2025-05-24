package com.puxinxiaolin.xiaolinshu.user.biz;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.puxinxiaolin.xiaolinshu.user.biz.domain.mapper")
public class XiaolinshuUserBizApplication {

    public static void main(String[] args) {
        SpringApplication.run(XiaolinshuUserBizApplication.class, args);
    }
    
}
