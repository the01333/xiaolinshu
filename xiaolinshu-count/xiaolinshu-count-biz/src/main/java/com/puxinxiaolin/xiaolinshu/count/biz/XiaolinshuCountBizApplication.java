package com.puxinxiaolin.xiaolinshu.count.biz;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.puxinxiaolin.xiaolinshu.count.biz.domain.mapper")
public class XiaolinshuCountBizApplication {

    public static void main(String[] args) {
        SpringApplication.run(XiaolinshuCountBizApplication.class, args);
    }
    
}
