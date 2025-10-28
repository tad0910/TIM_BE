package com.tim.appTim.config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import java.util.concurrent.Executor;

@Configuration
@EnableAsync

public class AsyncConfig {
    @Bean(name = "linkPreviewTaskExecutor")
    public Executor linkPreviewTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);      // 2 threads mặc định
        executor.setMaxPoolSize(5);       // Tối đa 5 threads
        executor.setQueueCapacity(100);   // Queue 100 tasks
        executor.setThreadNamePrefix("link-preview-");
        executor.initialize();
        return executor;
    }
}