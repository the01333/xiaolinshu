package com.puxinxiaolin.xiaolinshu.comment.biz;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.retry.annotation.EnableRetry;

@SpringBootApplication
@MapperScan("com.puxinxiaolin.xiaolinshu.comment.biz.domain.mapper")
@EnableRetry
public class XiaolinshuCommentBizApplication {

    public static void main(String[] args) {
        SpringApplication.run(XiaolinshuCommentBizApplication.class, args);
    }
    
}
