package com.puxinxiaolin.xiaolinshu.auth;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Slf4j
@SpringBootTest
public class ThreadPoolTaskExecutorTests {
    
    @Resource
    private ThreadPoolTaskExecutor threadPoolTaskExecutor;
    
    @Test
    void testSubmit() {
        threadPoolTaskExecutor.submit(() -> log.info("异步线程中说: 作者是普信小林..."));
    }
    
}
