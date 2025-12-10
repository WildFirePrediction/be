package com.capstone25.WildFirePrediction.service;

import com.capstone25.WildFirePrediction.config.SafetyDataProperties;
import com.capstone25.WildFirePrediction.dto.response.WeatherWarningApiResponse;
import com.capstone25.WildFirePrediction.global.code.status.ErrorStatus;
import com.capstone25.WildFirePrediction.global.exception.handler.ExceptionHandler;
import io.netty.handler.timeout.ReadTimeoutException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@Service
@RequiredArgsConstructor
@Slf4j
public class WeatherWarningApiService {

    private final WebClient safetyDataWebClient;
    private final SafetyDataProperties safetyDataProperties;

    private SafetyDataProperties.ApiConfig getConfig() {
        SafetyDataProperties.ApiConfig config =
                safetyDataProperties.getApis().get("weather-warning");
        if (config == null) {
            log.error("weather-warning API 설정을 찾을 수 없습니다. application.yml을 확인하세요.");
            throw new ExceptionHandler(ErrorStatus.WEATHER_WARNING_API_CONFIG_MISSING);
        }
        return config;
    }

    // 기상특보 통보문 원시 응답 JSON 조회 (테스트용)
    public String fetchWeatherWarningRaw(int pageNo, String startDate) {
        SafetyDataProperties.ApiConfig config = getConfig();

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
    public WeatherWarningApiResponse fetchWeatherWarningPage(int pageNo, String startDate) {
        SafetyDataProperties.ApiConfig config = getConfig();

        try {
            log.info("기상특보 통보문 API 호출 시작 - 페이지: {}, 시작일: {}", pageNo, startDate);

            WeatherWarningApiResponse response = safetyDataWebClient.get()
                    .uri(uriBuilder -> {
                        var builder = uriBuilder
                                .path(config.getPath())
                                .queryParam("serviceKey", config.getServiceKey())
                                .queryParam("pageNo", pageNo)
                                .queryParam("numOfRows", config.getPageSize())
                                .queryParam("returnType", "json");
                        if (startDate != null && !startDate.isBlank()) {
                            builder.queryParam("inqDt", startDate);
                        }
                        return builder.build();
                    })
                    .retrieve()
                    .onStatus(status -> status.is4xxClientError(),
                            clientResponse -> clientResponse.bodyToMono(String.class)
                                    .map(errorBody -> {
                                        log.error("[기상특보] 클라이언트 에러 - 상태코드: {}, 응답: {}",
                                                clientResponse.statusCode(), errorBody);
                                        return new ExceptionHandler(ErrorStatus.WEATHER_WARNING_API_CALL_FAILED);
                                    }))
                    .onStatus(status -> status.is5xxServerError(),
                            clientResponse -> clientResponse.bodyToMono(String.class)
                                    .map(errorBody -> {
                                        log.error("[기상특보] 서버 에러 - 상태코드: {}, 응답: {}",
                                                clientResponse.statusCode(), errorBody);
                                        return new ExceptionHandler(ErrorStatus.WEATHER_WARNING_API_CALL_FAILED);
                                    }))
                    .bodyToMono(WeatherWarningApiResponse.class)
                    .block();

            if (response == null) {
                log.warn("[기상특보] API 응답이 null입니다 - 페이지: {}", pageNo);
                throw new ExceptionHandler(ErrorStatus.WEATHER_WARNING_API_CALL_FAILED);
            }

            if (response.getHeader() == null || !"00".equals(response.getHeader().getResultCode())) {
                String errorCode = response.getHeader() != null ? response.getHeader().getResultCode() : "NULL_HEADER";
                String errorMsg = response.getHeader() != null ? response.getHeader().getResultMsg() : "응답 헤더가 없습니다.";
                log.error("[기상특보] API 응답 오류 - 코드: {}, 메시지: {}", errorCode, errorMsg);
                throw new ExceptionHandler(ErrorStatus.WEATHER_WARNING_API_CALL_FAILED);
            }

            log.info("[기상특보] API 호출 성공 - totalCount: {}, pageNo: {}, body size: {}",
                    response.getTotalCount(), pageNo,
                    response.getBody() != null ? response.getBody().size() : 0);

            return response;

        } catch (ReadTimeoutException e) {
            log.error("[기상특보] API 호출 타임아웃", e);
            throw new ExceptionHandler(ErrorStatus.WEATHER_WARNING_API_CALL_FAILED);
        } catch (WebClientResponseException e) {
            log.error("[기상특보] API HTTP 오류 - 상태코드: {}, 응답: {}",
                    e.getStatusCode(), e.getResponseBodyAsString());
            throw new ExceptionHandler(ErrorStatus.WEATHER_WARNING_API_CALL_FAILED);
        } catch (Exception e) {
            log.error("[기상특보] API 호출 중 예외 발생", e);
            throw new ExceptionHandler(ErrorStatus.WEATHER_WARNING_API_CALL_FAILED);
        }
    }
}
