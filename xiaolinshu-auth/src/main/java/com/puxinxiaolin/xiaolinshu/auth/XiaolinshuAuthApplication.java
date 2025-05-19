package com.puxinxiaolin.xiaolinshu.auth;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
    
@SpringBootApplication
@MapperScan("com.puxinxiaolin.xiaolinshu.auth.domain.mapper")
public class XiaolinshuAuthApplication {

    public static void main(String[] args) {
        SpringApplication.run(XiaolinshuAuthApplication.class, args);
    }
    
}
