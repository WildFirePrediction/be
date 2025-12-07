package com.capstone25.WildFirePrediction.config;

import java.util.concurrent.Executor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@EnableAsync    // 비동기 작업 활성화
public class AsyncConfig {

    // AI 예측 비동기 작업 처리기 설정
    @Bean(name = "aiPredictionExecutor")
    public Executor aiPredictionExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);    // 동시에 처리하는 최소 스레드 수
        executor.setMaxPoolSize(10);    // 동시에 처리하는 최대 스레드 수
        executor.setQueueCapacity(50);  // 큐의 최대 용량
        executor.setThreadNamePrefix("ai-prediction-async-");   // 스레드 이름 접두사
        executor.setWaitForTasksToCompleteOnShutdown(true);     // 종료 시 작업 완료 대기
        executor.setAwaitTerminationSeconds(60);    // 최대 대기 시간
        executor.initialize();
        return executor;
    }
}
