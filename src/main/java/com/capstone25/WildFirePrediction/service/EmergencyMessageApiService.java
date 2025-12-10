package com.capstone25.WildFirePrediction.service;

import com.capstone25.WildFirePrediction.config.SafetyDataProperties;
import com.capstone25.WildFirePrediction.dto.response.EmergencyMessageApiResponse;
import com.capstone25.WildFirePrediction.global.code.status.ErrorStatus;
import com.capstone25.WildFirePrediction.global.exception.handler.ExceptionHandler;
import io.netty.handler.timeout.ReadTimeoutException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmergencyMessageApiService {

    private final WebClient safetyDataWebClient;
    private final SafetyDataProperties safetyDataProperties;

    // api에서 한 페이지 조회
    public EmergencyMessageApiResponse fetchEmergencyMessagePage(int pageNo, String startDate, String region) {
        // 1. yml에서 emergency-message api 설정 가져오기
        SafetyDataProperties.ApiConfig config = safetyDataProperties.getApis().get("emergency-message");

        if (config == null) {
            log.error("emergency-message API 설정을 찾을 수 없습니다. application.yml을 확인하세요.");
            throw new ExceptionHandler(ErrorStatus.EMERGENCY_API_CONFIG_MISSING);
        }

        try {
            log.info("긴급재난문자 API 호출 시작 - 페이지: {}, 시작일: {}, 지역: {}",
                    pageNo, startDate, region);

            // 2. WebClient로 GET 요청 (shelter와 동일)
            EmergencyMessageApiResponse response = safetyDataWebClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path(config.getPath())
                            .queryParam("serviceKey", config.getServiceKey())
                            .queryParam("pageNo", pageNo)
                            .queryParam("numOfRows", config.getPageSize())
                            .queryParam("returnType", "json")
                            .queryParam("crtDt", startDate)  // 조회시작일자
                            .queryParam("rgnNm", region)     // 지역명
                            .build())
                    .retrieve()
                    .onStatus(status -> status.is4xxClientError(),
                            clientResponse -> clientResponse.bodyToMono(String.class)
                                    .map(errorBody -> {
                                        log.error("클라이언트 에러 - 상태코드: {}, 응답: {}",
                                                clientResponse.statusCode(), errorBody);
                                        if (clientResponse.statusCode().value() == 401 ||
                                                clientResponse.statusCode().value() == 403) {
                                            return new ExceptionHandler(ErrorStatus.EMERGENCY_API_SERVICE_KEY_INVALID);
                                        }
                                        return new ExceptionHandler(ErrorStatus.EMERGENCY_API_CALL_FAILED);
                                    }))
                    .onStatus(status -> status.is5xxServerError(),
                            clientResponse -> clientResponse.bodyToMono(String.class)
                                    .map(errorBody -> {
                                        log.error("서버 에러 - 상태코드: {}, 응답: {}",
                                                clientResponse.statusCode(), errorBody);
                                        return new ExceptionHandler(ErrorStatus.EMERGENCY_API_CALL_FAILED);
                                    }))
                    .bodyToMono(EmergencyMessageApiResponse.class)
                    .block();

            // 3. API 응답 검증 (shelter와 동일)
            if (response == null) {
                log.warn("긴급재난문자 API 응답이 null입니다 - 페이지: {}", pageNo);
                return response;
            }

            if (response.getHeader() == null || !"00".equals(response.getHeader().getResultCode())) {
                String errorCode = response.getHeader() != null ? response.getHeader().getResultCode() : "NULL_HEADER";
                String errorMsg = response.getHeader() != null ? response.getHeader().getResultMsg() : "응답 헤더가 없습니다.";

                log.error("API 응답 오류 - 코드: {}, 메시지: {}", errorCode, errorMsg);
                throw new ExceptionHandler(ErrorStatus.EMERGENCY_API_CALL_FAILED);
            }

            log.info("긴급재난문자 API 호출 성공 - 총 {}건 중 페이지 {}: {}건",
                    response.getTotalCount(), pageNo,
                    response.getBody() != null ? response.getBody().size() : 0);

            return response;

        } catch (ReadTimeoutException e) {
            log.error("긴급재난문자 API 호출 타임아웃", e);
            throw new ExceptionHandler(ErrorStatus.EMERGENCY_API_TIMEOUT);
        } catch (WebClientResponseException e) {
            log.error("API 호출 중 HTTP 오류 - 상태코드: {}, 응답: {}",
                    e.getStatusCode(), e.getResponseBodyAsString());
            throw new ExceptionHandler(ErrorStatus._INTERNAL_SERVER_ERROR);
        } catch (Exception e) {
            log.error("긴급재난문자 API 호출 중 예외 발생", e);
            throw new ExceptionHandler(ErrorStatus._INTERNAL_SERVER_ERROR);
        }
    }
}
