package com.puxinxiaolin.xiaolinshu.data.align;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.puxinxiaolin.xiaolinshu.data.align.domain.mapper")
public class XiaolinshuDataAlignApplication {

    public static void main(String[] args) {
        SpringApplication.run(XiaolinshuDataAlignApplication.class, args);
    }
    
}
