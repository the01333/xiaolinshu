package com.puxinxiaolin.xiaolinshu.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients(basePackages = "com.puxinxiaolin.xiaolinshu")
public class XiaolinshuAuthApplication {

    public static void main(String[] args) {
        SpringApplication.run(XiaolinshuAuthApplication.class, args);
    }
    
}
