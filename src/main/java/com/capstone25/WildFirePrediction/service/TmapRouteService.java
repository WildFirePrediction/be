package com.capstone25.WildFirePrediction.service;

import com.capstone25.WildFirePrediction.dto.request.RouteRequest;
import com.capstone25.WildFirePrediction.dto.request.TmapApiRequest;
import com.capstone25.WildFirePrediction.dto.response.RouteResponse;
import com.capstone25.WildFirePrediction.dto.response.TmapApiResponse;
import com.capstone25.WildFirePrediction.global.code.status.ErrorStatus;
import com.capstone25.WildFirePrediction.global.exception.handler.ExceptionHandler;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Service
@RequiredArgsConstructor
public class TmapRouteService {

    @Value("${tmap.api.key}")
    private String tmapApiKey;

    private static final String TMAP_API_URL =
            "https://apis.openapi.sk.com/tmap/routes/pedestrian?version=1";

    private final RestTemplate restTemplate;

    // TMAP API 호출해서 보행자 경로 받아오기
    public RouteResponse getTmapRoute(RouteRequest request) {
        // 1. TMAP API 요청 생성
        TmapApiRequest tmapRequest = TmapApiRequest.builder()
                .startX(request.getStartLon())
                .startY(request.getStartLat())
                .endX(request.getEndLon())
                .endY(request.getEndLat())
                .startName("현재 위치") // 기본값
                .endName("대피소")     // 기본값
                .reqCoordType("WGS84GEO")
                .resCoordType("WGS84GEO")
                .searchOption(0) // 추천 경로
                .build();

        // 2. TMAP API 호출
        TmapApiResponse tmapResponse = callTmapApi(tmapRequest);

        // 3. 응답 파싱
        return parseRouteResponse(tmapResponse);
    }

    // TMAP API 실제 호출
    private TmapApiResponse callTmapApi(TmapApiRequest request) {
        try {
            // Header 설정
            HttpHeaders headers = new HttpHeaders();
            headers.set("appKey", tmapApiKey);
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            headers.set("Accept", "application/json");

            // Body
            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("startX", String.valueOf(request.getStartX()));
            body.add("startY", String.valueOf(request.getStartY()));
            body.add("endX", String.valueOf(request.getEndX()));
            body.add("endY", String.valueOf(request.getEndY()));
            body.add("startName", urlEncode(request.getStartName()));
            body.add("endName", urlEncode(request.getEndName()));
            body.add("reqCoordType", request.getReqCoordType());
            body.add("resCoordType", request.getResCoordType());
            body.add("searchOption", String.valueOf(request.getSearchOption()));

            if (request.getPassList() != null) {
                body.add("passList", request.getPassList());
            }

            HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(body, headers);

            // API 호출
            ResponseEntity<TmapApiResponse> response = restTemplate.exchange(
                    TMAP_API_URL,
                    HttpMethod.POST,
                    entity,
                    TmapApiResponse.class
            );

            if (response.getStatusCode() != HttpStatus.OK || response.getBody() == null) {
                throw new ExceptionHandler(ErrorStatus.TMAP_ROUTE_API_FAILED);
            }

            return response.getBody();
        } catch (Exception e) {
            log.error("TMAP API 호출 실패 : ", e);
            throw new ExceptionHandler(ErrorStatus.TMAP_ROUTE_API_FAILED);
        }
    }

    // TMAP 응답 파싱
    private RouteResponse parseRouteResponse(TmapApiResponse tmapResponse) {
        Integer totalDistance = null;
        Integer totalTime = null;
        List<List<Double>> path = new ArrayList<>();

        for(TmapApiResponse.Feature feature : tmapResponse.getFeatures()) {
            TmapApiResponse.Properties props = feature.getProperties();

            // 총 거리/시간 추출 (SP = 출발지에서만)
            if ("SP".equals(props.getPointType())) {
                totalDistance = props.getTotalDistance();
                totalTime = props.getTotalTime();
            }

            // 경로 좌표 추출 (LineString만)
            if ("LineString".equals(feature.getGeometry().getType())) {
                @SuppressWarnings("unchecked")
                List<List<Double>> coordinates =
                        (List<List<Double>>) feature.getGeometry().getCoordinates();
                path.addAll(coordinates);
            }
        }

        return RouteResponse.builder()
                .totalDistance(totalDistance)
                .totalTime(totalTime != null ? (int) Math.round(totalTime / 60.0) : null)
                .path(path)
                .message("경로 조회 성공")
                .build();
    }

    // UTF-8 인코딩
    private String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
