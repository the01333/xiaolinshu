package com.puxinxiaolin.xiaolinshu.note.biz;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@MapperScan("com.puxinxiaolin.xiaolinshu.note.biz.domain.mapper")
@EnableFeignClients(basePackages = "com.puxinxiaolin.xiaolinshu")
public class XiaolinshuNoteBizApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(XiaolinshuNoteBizApplication.class, args);
    }
    
}
