package com.puxinxiaolin.xiaolinshu.auth.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * @Description: 自定义线程工厂
 * @Author: YCcLin
 * @Date: 2025/5/21 15:06
 */
@Configuration
public class ThreadPoolConfig {

    @Bean("taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);
        executor.setMaxPoolSize(50);
        // 线程活跃时间（s）
        executor.setKeepAliveSeconds(30);
        // 队列容量
        executor.setQueueCapacity(200);
        // 线程池名前缀
        executor.setThreadNamePrefix("AuthExecutor-");

        // 拒绝策略: 由调用线程处理（一般为主线程）
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());

        // 等待所有任务执行完毕后再关闭线程池
        executor.setWaitForTasksToCompleteOnShutdown(true);
        // 设置等待时间，如果超过这个时间还没有销毁就强制销毁，以确保应用最后能够被关闭，而不是被没有完成的任务阻塞
        executor.setAwaitTerminationSeconds(60);

        executor.initialize();
        return executor;
    }

}
