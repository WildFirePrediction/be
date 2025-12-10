package com.capstone25.WildFirePrediction.service;

import com.capstone25.WildFirePrediction.config.SafetyDataProperties;
import com.capstone25.WildFirePrediction.dto.response.PublicApiResponse;
import com.capstone25.WildFirePrediction.dto.ShelterDto;
import com.capstone25.WildFirePrediction.global.code.status.ErrorStatus;
import com.capstone25.WildFirePrediction.global.exception.handler.ExceptionHandler;
import io.netty.handler.timeout.ReadTimeoutException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@Slf4j
@Service
@RequiredArgsConstructor
public class ShelterApiService {

    private final WebClient safetyDataWebClient;
    private final SafetyDataProperties safetyDataProperties;

    // api에서 대피소 정보 한 페이지 조회 (페이징)
    public PublicApiResponse.PagedResponse<ShelterDto> fetchShelterPage(int pageNo) {
        // 1. yml에서 shelter api 설정 가져오기
        SafetyDataProperties.ApiConfig shelterConfig = safetyDataProperties.getApis().get("shelter");

        if (shelterConfig == null) {
            log.error("shelter API 설정을 찾을 수 없습니다. application.yml을 확인하세요.");
            throw new ExceptionHandler(ErrorStatus.SHELTER_API_CONFIG_MISSING);
        }

        try {
            log.info("대피소 API 호출 시작 - 페이지: {}, 페이지당 개수: {}", pageNo, shelterConfig.getPageSize());

            // 2. WebClient로 GET 요청
            PublicApiResponse.PagedResponse<ShelterDto> response = safetyDataWebClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path(shelterConfig.getPath())
                            .queryParam("serviceKey", shelterConfig.getServiceKey())
                            .queryParam("pageNo", pageNo)
                            .queryParam("numOfRows", shelterConfig.getPageSize())
                            .queryParam("returnType", "json")  // JSON 형식으로 응답 요청
                            .build())
                    .retrieve() // 응답 수신
                    .onStatus(status -> status.is4xxClientError(),  // 4xx 에러 처리
                            clientResponse -> clientResponse.bodyToMono(String.class)
                                    .map(errorBody -> {
                                        log.error("클라이언트 에러 - 상태코드: {}, 응답: {}",
                                                clientResponse.statusCode(), errorBody);
                                        // 401, 403 등 인증 관련은 SERVICE_KEY_INVALID
                                        if (clientResponse.statusCode().value() == 401 ||
                                                clientResponse.statusCode().value() == 403) {
                                            return new ExceptionHandler(ErrorStatus.SHELTER_API_SERVICE_KEY_INVALID);
                                        }
                                        return new ExceptionHandler(ErrorStatus.SHELTER_API_CALL_FAILED);
                                    }))
                    .onStatus(status -> status.is5xxServerError(),  // 5xx 에러 처리
                            clientResponse -> clientResponse.bodyToMono(String.class)
                                    .map(errorBody -> {
                                        log.error("서버 에러 - 상태코드: {}, 응답: {}",
                                                clientResponse.statusCode(), errorBody);
                                        return new ExceptionHandler(ErrorStatus.SHELTER_API_CALL_FAILED);
                                    }))
                    .bodyToMono(new ParameterizedTypeReference<PublicApiResponse.PagedResponse<ShelterDto>>() {})  // JSON을 DTO로 자동 변환
                    .block();  // 동기 방식으로 결과 대기 (비동기는 나중에 적용 가능)

            // 3. API 응답 검증
            if (response == null) {
                log.warn("대피소 API 응답이 null입니다 - 페이지: {}", pageNo);
                return response;
            }

            if (!"00".equals(response.getHeader().getResultCode())) {
                log.error("API 응답 오류 - 코드: {}, 메시지: {}",
                        response.getHeader().getResultCode(), response.getHeader().getResultMsg());
                throw new ExceptionHandler(ErrorStatus.SHELTER_API_CALL_FAILED);
            }

            List<ShelterDto> dataList = response.getBody();
            log.info("대피소 API 호출 성공 - 총 {}건 중 페이지 {}: {}건",
                    response.getTotalCount(), pageNo, dataList != null ? dataList.size() : 0);

            return response;
        } catch (ReadTimeoutException e) {
            log.error("대피소 API 호출 타임아웃", e);
            throw new ExceptionHandler(ErrorStatus.SHELTER_API_TIMEOUT);
        } catch (WebClientResponseException e) {
            log.error("API 호출 중 HTTP 오류 발생 - 상태코드: {}, 응답: {}",
                    e.getStatusCode(), e.getResponseBodyAsString());
            throw new ExceptionHandler(ErrorStatus.SHELTER_API_CALL_FAILED);
        } catch (Exception e) {
            log.error("대피소 API 호출 중 예외 발생", e);
            if (e.getMessage() != null && e.getMessage().contains("Json")) {
                throw new ExceptionHandler(ErrorStatus.SHELTER_API_RESPONSE_INVALID);
            }
            throw new ExceptionHandler(ErrorStatus._INTERNAL_SERVER_ERROR);
        }
    }
}
