package com.capstone25.WildFirePrediction.service;

import com.capstone25.WildFirePrediction.config.SafetyDataProperties;
import com.capstone25.WildFirePrediction.global.code.status.ErrorStatus;
import com.capstone25.WildFirePrediction.global.exception.handler.ExceptionHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
@RequiredArgsConstructor
@Slf4j
public class WeatherWarningApiService {

    private final WebClient safetyDataWebClient;
    private final SafetyDataProperties safetyDataProperties;

    // 기상특보 통보문 원시 응답 JSON 조회 (테스트용)
    public String fetchWeatherWarningRaw(int pageNo, String startDate) {
        SafetyDataProperties.ApiConfig config =
                safetyDataProperties.getApis().get("weather-warning");

        if (config == null) {
            log.error("weather-warning API 설정을 찾을 수 없습니다.");
            throw new ExceptionHandler(ErrorStatus.WEATHER_WARNING_API_CONFIG_MISSING);
        }

        try {
            log.info("기상특보 통보문 API 호출 시작 - 페이지: {}, 시작일: {}", pageNo, startDate);

            String rawJson = safetyDataWebClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path(config.getPath())
                            .queryParam("serviceKey", config.getServiceKey())
                            .queryParam("pageNo", pageNo)
                            .queryParam("numOfRows", config.getPageSize())
                            .queryParam("returnType", "json")
                            .queryParam("inqDt", startDate)  // 재난문자와 동일하게 날짜 필터부터 시도
                            .build())
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            log.info("기상특보 통보문 API 원시 응답 수신 완료");
            return rawJson;
        } catch (Exception e) {
            log.error("기상특보 통보문 API 원시 호출 실패", e);
            throw new ExceptionHandler(ErrorStatus.WEATHER_WARNING_API_CALL_FAILED);
        }
    }
}
