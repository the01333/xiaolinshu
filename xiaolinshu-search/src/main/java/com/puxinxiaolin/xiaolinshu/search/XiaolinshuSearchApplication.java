package com.puxinxiaolin.xiaolinshu.search;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class XiaolinshuSearchApplication {

    public static void main(String[] args) {
        SpringApplication.run(XiaolinshuSearchApplication.class, args);
    }
    
}
