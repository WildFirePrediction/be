package com.capstone25.WildFirePrediction.service;

import com.capstone25.WildFirePrediction.config.SafetyDataProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmergencyMessageApiService {

    private final WebClient safetyDataWebClient;
    private final SafetyDataProperties safetyDataProperties;

    // (임시) 재난문자 API 원본 응답 확인 (String)
    public void testDisasterMessageApi() {
        SafetyDataProperties.ApiConfig config = safetyDataProperties.getApis().get("emergency-message");

        if (config == null) {
            log.error("disaster-message API 설정 없음");
            return;
        }

        try {
            log.info("=== 재난문자 API 호출 시작 ===");

            String rawResponse = safetyDataWebClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path(config.getPath())
                            .queryParam("serviceKey", config.getServiceKey())
                            .queryParam("pageNo", 1)
                            .queryParam("numOfRows", 10)
                            .queryParam("type", "json")  // JSON 형식!
                            .build())
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            log.info("=== 재난문자 API 원본 응답 ===");
            log.info(rawResponse);
            log.info("=== 응답 길이: {} ===", rawResponse != null ? rawResponse.length() : 0);

        } catch (Exception e) {
            log.error("재난문자 API 호출 실패", e);
        }
    }
}
