package com.capstone25.WildFirePrediction.service;

import com.capstone25.WildFirePrediction.config.SafetyDataProperties;
import com.capstone25.WildFirePrediction.dto.response.EarthquakeApiResponse;
import com.capstone25.WildFirePrediction.dto.response.WildFireApiResponse;
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
public class DisasterInfoApiService {
    private final WebClient safetyDataWebClient;
    private final SafetyDataProperties safetyDataProperties;

    // 산불 api - 한 페이지 조회
    public WildFireApiResponse fetchWildFirePage(int pageNo, String startDate) {
        SafetyDataProperties.ApiConfig config = safetyDataProperties.getApis().get("wildfire");

        if (config == null) {
            log.error("wildfire API 설정을 찾을 수 없습니다. application.yml을 확인하세요.");
            throw new ExceptionHandler(ErrorStatus.WILDFIRE_API_CONFIG_MISSING);
        }

        try {
            log.info("산불 API 호출 시작 - 페이지: {}, 시작일: {}", pageNo, startDate);

            WildFireApiResponse response = safetyDataWebClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path(config.getPath())
                            .queryParam("serviceKey", config.getServiceKey())
                            .queryParam("pageNo", pageNo)
                            .queryParam("numOfRows", config.getPageSize())
                            .queryParam("returnType", "json")
                            .queryParam("startDt", startDate)
                            .build())
                    .retrieve()
                    .onStatus(status -> status.is4xxClientError(),
                            clientResponse -> clientResponse.bodyToMono(String.class)
                                    .map(errorBody -> {
                                        log.error("클라이언트 에러 - 상태코드: {}, 응답: {}",
                                                clientResponse.statusCode(), errorBody);
                                        if (clientResponse.statusCode().value() == 401 ||
                                                clientResponse.statusCode().value() == 403) {
                                            return new ExceptionHandler(ErrorStatus.WILDFIRE_API_SERVICE_KEY_INVALID);
                                        }
                                        return new ExceptionHandler(ErrorStatus.WILDFIRE_API_CALL_FAILED);
                                    }))
                    .onStatus(status -> status.is5xxServerError(),
                            clientResponse -> clientResponse.bodyToMono(String.class)
                                    .map(errorBody -> {
                                        log.error("서버 에러 - 상태코드: {}, 응답: {}",
                                                clientResponse.statusCode(), errorBody);
                                        return new ExceptionHandler(ErrorStatus.WILDFIRE_API_CALL_FAILED);
                                    }))
                    .bodyToMono(WildFireApiResponse.class)
                    .block();

            if (response == null) {
                log.warn("산불 API 응답이 null입니다 - 페이지: {}", pageNo);
                return response;
            }

            if (response.getHeader() == null || !"00".equals(response.getHeader().getResultCode())) {
                String errorCode = response.getHeader() != null ? response.getHeader().getResultCode() : "NULL_HEADER";
                String errorMsg = response.getHeader() != null ? response.getHeader().getResultMsg() : "응답 헤더가 없습니다.";

                log.error("API 응답 오류 - 코드: {}, 메시지: {}", errorCode, errorMsg);
                throw new ExceptionHandler(ErrorStatus.WILDFIRE_API_CALL_FAILED);
            }

            log.info("산불 API 호출 성공 - 총 {}건 중 페이지 {}: {}건",
                    response.getTotalCount(), pageNo,
                    response.getBody() != null ? response.getBody().size() : 0);

            return response;

        } catch (ReadTimeoutException e) {
            log.error("산불 API 호출 타임아웃", e);
            throw new ExceptionHandler(ErrorStatus.WILDFIRE_API_TIMEOUT);
        } catch (WebClientResponseException e) {
            log.error("API 호출 중 HTTP 오류 - 상태코드: {}, 응답: {}",
                    e.getStatusCode(), e.getResponseBodyAsString());
            throw new ExceptionHandler(ErrorStatus._INTERNAL_SERVER_ERROR);
        } catch (Exception e) {
            log.error("산불 API 호출 중 예외 발생", e);
            throw new ExceptionHandler(ErrorStatus._INTERNAL_SERVER_ERROR);
        }
    }

    // 지진 api - 한 페이지 조회
    public EarthquakeApiResponse fetchEarthquakePage(int pageNo) {
        SafetyDataProperties.ApiConfig config = safetyDataProperties.getApis().get("earthquake");

        if (config == null) {
            log.error("earthquake API 설정을 찾을 수 없습니다. application.yml을 확인하세요.");
            throw new ExceptionHandler(ErrorStatus.EARTHQUAKE_API_CONFIG_MISSING);
        }

        try {
            log.info("지진 API 호출 시작 - 페이지: {}", pageNo);

            EarthquakeApiResponse response = safetyDataWebClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path(config.getPath())
                            .queryParam("serviceKey", config.getServiceKey())
                            .queryParam("pageNo", pageNo)
                            .queryParam("numOfRows", config.getPageSize())
                            .queryParam("returnType", "json")
                            .build())
                    .retrieve()
                    .onStatus(status -> status.is4xxClientError(),
                            clientResponse -> clientResponse.bodyToMono(String.class)
                                    .map(errorBody -> {
                                        log.error("클라이언트 에러 - 상태코드: {}, 응답: {}",
                                                clientResponse.statusCode(), errorBody);
                                        if (clientResponse.statusCode().value() == 401 ||
                                                clientResponse.statusCode().value() == 403) {
                                            return new ExceptionHandler(ErrorStatus.EARTHQUAKE_API_SERVICE_KEY_INVALID);
                                        }
                                        return new ExceptionHandler(ErrorStatus.EARTHQUAKE_API_CALL_FAILED);
                                    }))
                    .onStatus(status -> status.is5xxServerError(),
                            clientResponse -> clientResponse.bodyToMono(String.class)
                                    .map(errorBody -> {
                                        log.error("서버 에러 - 상태코드: {}, 응답: {}",
                                                clientResponse.statusCode(), errorBody);
                                        return new ExceptionHandler(ErrorStatus.EARTHQUAKE_API_CALL_FAILED);
                                    }))
                    .bodyToMono(EarthquakeApiResponse.class)
                    .block();

            if (response == null) {
                log.warn("지진 API 응답이 null입니다 - 페이지: {}", pageNo);
                return response;
            }

            if (response.getHeader() == null || !"00".equals(response.getHeader().getResultCode())) {
                String errorCode = response.getHeader() != null ? response.getHeader().getResultCode() : "NULL_HEADER";
                String errorMsg = response.getHeader() != null ? response.getHeader().getResultMsg() : "응답 헤더가 없습니다.";

                log.error("API 응답 오류 - 코드: {}, 메시지: {}", errorCode, errorMsg);
                throw new ExceptionHandler(ErrorStatus.EARTHQUAKE_API_CALL_FAILED);
            }

            log.info("지진 API 호출 성공 - 총 {}건 중 페이지 {}: {}건",
                    response.getTotalCount(), pageNo,
                    response.getBody() != null ? response.getBody().size() : 0);

            return response;

        } catch (ReadTimeoutException e) {
            log.error("지진 API 호출 타임아웃", e);
            throw new ExceptionHandler(ErrorStatus.EARTHQUAKE_API_TIMEOUT);
        } catch (WebClientResponseException e) {
            log.error("API 호출 중 HTTP 오류 - 상태코드: {}, 응답: {}",
                    e.getStatusCode(), e.getResponseBodyAsString());
            throw new ExceptionHandler(ErrorStatus._INTERNAL_SERVER_ERROR);
        } catch (Exception e) {
            log.error("지진 API 호출 중 예외 발생", e);
            throw new ExceptionHandler(ErrorStatus._INTERNAL_SERVER_ERROR);
        }
    }
}
