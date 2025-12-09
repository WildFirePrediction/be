package com.capstone25.WildFirePrediction.config;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

@Configuration
@RequiredArgsConstructor
public class WebClientConfig {

    // yml을 매핑한 설정 객체 주입
    private final SafetyDataProperties safetyDataProperties;

    @Bean
    public WebClient safetyDataWebClient() {
        // HttpClient 설정: 연결 타임아웃과 응답 타임아웃 설정
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)  // 연결 타임아웃: 5초
                .responseTimeout(Duration.ofSeconds(10))  // 응답 타임아웃: 10초
                .doOnConnected(conn -> conn
                        .addHandlerLast(new ReadTimeoutHandler(10, TimeUnit.SECONDS))  // 읽기 타임아웃
                        .addHandlerLast(new WriteTimeoutHandler(10, TimeUnit.SECONDS)));  // 쓰기 타임아웃

        // 재난안전데이터 공유플랫폼 전용 WebClient
        return WebClient.builder()
                .baseUrl(safetyDataProperties.getBaseUrl())  // 공통 base URL 설정
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }
}
