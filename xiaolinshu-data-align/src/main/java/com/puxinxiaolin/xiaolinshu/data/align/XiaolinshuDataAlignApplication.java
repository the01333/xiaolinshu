package com.puxinxiaolin.xiaolinshu.data.align;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@MapperScan("com.puxinxiaolin.xiaolinshu.data.align.domain.mapper")
@EnableFeignClients(basePackages = "com.puxinxiaolin.xiaolinshu")
public class XiaolinshuDataAlignApplication {

    public static void main(String[] args) {
        SpringApplication.run(XiaolinshuDataAlignApplication.class, args);
    }
    
}
