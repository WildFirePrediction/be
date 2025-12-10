package com.capstone25.WildFirePrediction.service;

import com.capstone25.WildFirePrediction.config.SafetyDataProperties;
import com.capstone25.WildFirePrediction.dto.response.PublicApiResponse;
import com.capstone25.WildFirePrediction.dto.response.PublicApiResponse.PagedResponse;
import com.capstone25.WildFirePrediction.global.code.status.ErrorStatus;
import com.capstone25.WildFirePrediction.global.exception.handler.ExceptionHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@Slf4j
@Service
@RequiredArgsConstructor
public class PublicApiService {

    private final WebClient safetyDataWebClient;
    private final SafetyDataProperties safetyDataProperties;

    // 공통 페이징 GET 호출
    public <T> PublicApiResponse.PagedResponse<T> fetchPaged(
            String apiKey,
            int pageNo,
            ParameterizedTypeReference<PagedResponse<T>> typeRef
    ) {
        SafetyDataProperties.ApiConfig config = safetyDataProperties.getApis().get(apiKey);

        if (config == null) {
            log.error("[{}] API 설정을 찾을 수 없습니다. application.yml을 확인하세요.", apiKey);
            throw new ExceptionHandler(ErrorStatus.SHELTER_API_CONFIG_MISSING);
        }

        try {
            log.info("[{}] API 호출 시작 - 페이지: {}, 페이지당 개수: {}", apiKey, pageNo, config.getPageSize());

            var uriSpec = safetyDataWebClient.get()
                    .uri(uriBuilder -> {
                        var builder = uriBuilder
                                .path(config.getPath())
                                .queryParam("serviceKey", config.getServiceKey())
                                .queryParam("pageNo", pageNo)
                                .queryParam("numOfRows", config.getPageSize());
                        // 대피소는 returnType, 재난/기상은 type 등으로 다를 수 있으니 분기
                        if ("shelter".equals(apiKey)) {
                            builder.queryParam("returnType", "json");
                        } else {
                            builder.queryParam("type", "json");
                        }
                        return builder.build();
                    });

            PublicApiResponse.PagedResponse<T> response = uriSpec
                    .retrieve()
                    .onStatus(status -> status.is4xxClientError(),
                            clientResponse -> clientResponse.bodyToMono(String.class)
                                    .map(errorBody -> {
                                        log.error("[{}] 클라이언트 에러 - 상태코드: {}, 응답: {}",
                                                apiKey, clientResponse.statusCode(), errorBody);
                                        if (clientResponse.statusCode().value() == 401 ||
                                                clientResponse.statusCode().value() == 403) {
                                            return new ExceptionHandler(ErrorStatus.SHELTER_API_SERVICE_KEY_INVALID);
                                        }
                                        return new ExceptionHandler(ErrorStatus.SHELTER_API_CALL_FAILED);
                                    }))
                    .onStatus(status -> status.is5xxServerError(),
                            clientResponse -> clientResponse.bodyToMono(String.class)
                                    .map(errorBody -> {
                                        log.error("[{}] 서버 에러 - 상태코드: {}, 응답: {}",
                                                apiKey, clientResponse.statusCode(), errorBody);
                                        return new ExceptionHandler(ErrorStatus.SHELTER_API_CALL_FAILED);
                                    }))
                    .bodyToMono(typeRef)
                    .block();

            if (response == null) {
                log.warn("[{}] API 응답이 null입니다 - 페이지: {}", apiKey, pageNo);
                return null;
            }

            if (response.getHeader() != null &&
                    !"00".equals(response.getHeader().getResultCode())) {
                log.error("[{}] API 응답 오류 - 코드: {}, 메시지: {}",
                        apiKey,
                        response.getHeader().getResultCode(),
                        response.getHeader().getResultMsg());
                throw new ExceptionHandler(ErrorStatus.SHELTER_API_CALL_FAILED);
            }

            return response;
        } catch (WebClientResponseException e) {
            log.error("[{}] API 호출 중 HTTP 오류 - 상태코드: {}, 응답: {}",
                    apiKey, e.getStatusCode(), e.getResponseBodyAsString());
            throw new ExceptionHandler(ErrorStatus.SHELTER_API_CALL_FAILED);
        } catch (Exception e) {
            log.error("[{}] API 호출 중 예외 발생", apiKey, e);
            if (e.getMessage() != null && e.getMessage().contains("Json")) {
                throw new ExceptionHandler(ErrorStatus.SHELTER_API_RESPONSE_INVALID);
            }
            throw new ExceptionHandler(ErrorStatus._INTERNAL_SERVER_ERROR);
        }
    }
}
