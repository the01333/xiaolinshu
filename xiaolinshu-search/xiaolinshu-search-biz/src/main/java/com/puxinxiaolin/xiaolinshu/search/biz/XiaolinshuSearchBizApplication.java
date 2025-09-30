package com.puxinxiaolin.xiaolinshu.search.biz;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@MapperScan("com.puxinxiaolin.xiaolinshu.search.biz.domain.mapper")
@EnableScheduling
public class XiaolinshuSearchBizApplication {

    public static void main(String[] args) {
        SpringApplication.run(XiaolinshuSearchBizApplication.class, args);
    }
    
}
