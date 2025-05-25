package com.puxinxiaolin.xiaolinshu.auth;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@MapperScan("com.puxinxiaolin.xiaolinshu.auth.domain.mapper")
@EnableFeignClients(basePackages = "com.puxinxiaolin.xiaolinshu")
public class XiaolinshuAuthApplication {

    public static void main(String[] args) {
        SpringApplication.run(XiaolinshuAuthApplication.class, args);
    }
    
}
