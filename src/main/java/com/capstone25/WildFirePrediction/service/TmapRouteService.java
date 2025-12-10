package com.capstone25.WildFirePrediction.service;

import com.capstone25.WildFirePrediction.domain.AIPredictedCell;
import com.capstone25.WildFirePrediction.dto.request.RouteRequest;
import com.capstone25.WildFirePrediction.dto.request.TmapApiRequest;
import com.capstone25.WildFirePrediction.dto.response.RouteResponse;
import com.capstone25.WildFirePrediction.dto.response.TmapApiResponse;
import com.capstone25.WildFirePrediction.global.code.status.ErrorStatus;
import com.capstone25.WildFirePrediction.global.exception.handler.ExceptionHandler;
import com.capstone25.WildFirePrediction.repository.AIPredictedCellRepository;
import com.capstone25.WildFirePrediction.util.GeoUtils;
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
    private final AIPredictedCellRepository aiPredictedCellRepository;

    // 안전 경로 조회
    public RouteResponse getSafeRoute(RouteRequest request) {
        log.info("안전 경로 조회 요청: ({}, {}) → ({}, {})",
                request.getStartLat(), request.getStartLon(),
                request.getEndLat(), request.getEndLon());

        // 1. 원본 경로 조회
        RouteResponse originalRoute = getOriginalRoute(request);

        // 2. 주변 위험 셀 조회 (바운딩 박스 활용)
        List<AIPredictedCell> dangerCells = findDangerCellsNearRoute(request);
        log.info("주변 위험 셀 {}개 발견", dangerCells.size());

        // 3. 안전 검사
        boolean isSafe = GeoUtils.isRouteSafe(originalRoute.getPath(), dangerCells);
        if (isSafe) {
            log.info("✅ 경로 안전!");
            return originalRoute;
        }

        log.warn("⚠️ 경로 위험! 우회 경로 탐색...");

        // 4. 우회 경로 반환 (DB 공간인덱스 없어도 동작)
        return findBypassRoute(request);
    }

    // TMAP API 호출해서 보행자 경로 받아오기
    public RouteResponse getOriginalRoute(RouteRequest request) {
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

    // 주변 위험 셀 조회 (바운딩 박스 활용)
    private List<AIPredictedCell> findDangerCellsNearRoute(RouteRequest request) {
        double padding = 0.01; // 1km 여유
        double minLat = Math.min(request.getStartLat(), request.getEndLat()) - padding;
        double maxLat = Math.max(request.getStartLat(), request.getEndLat()) + padding;
        double minLon = Math.min(request.getStartLon(), request.getEndLon()) - padding;
        double maxLon = Math.max(request.getStartLon(), request.getEndLon()) + padding;

        return aiPredictedCellRepository.findCellsInBoundingBox(minLat, maxLat, minLon, maxLon);
    }

    // 우회 경로 계산
    private RouteResponse findBypassRoute(RouteRequest request) {
        // 위험 셀들의 중심점 계산
        double centerLat = (request.getStartLat() + request.getEndLat()) / 2;
        double centerLon = (request.getStartLon() + request.getEndLon()) / 2;
        double delta = 0.0045; // 500m

        // 4방향 완전 우회
        String[] bypassStrategies = {
                String.format("%.6f,%.6f", centerLon,     centerLat + delta), // 북
                String.format("%.6f,%.6f", centerLon + delta, centerLat),     // 동
                String.format("%.6f,%.6f", centerLon,     centerLat - delta), // 남
                String.format("%.6f,%.6f", centerLon - delta, centerLat)      // 서
        };
        for (int i = 0; i < bypassStrategies.length; i++) {
            String passList = bypassStrategies[i];
            log.info("우회 방향 {} ({}): {}", i+1, getDirection(i), passList);

            TmapApiRequest bypassRequest = TmapApiRequest.builder()
                    .startX(request.getStartLon())
                    .startY(request.getStartLat())
                    .endX(request.getEndLon())
                    .endY(request.getEndLat())
                    .passList(passList)
                    .startName("현재 위치")
                    .endName("대피소")
                    .reqCoordType("WGS84GEO")
                    .resCoordType("WGS84GEO")
                    .searchOption(0)
                    .build();

            RouteResponse bypassRoute = parseRouteResponse(callTmapApi(bypassRequest));

            // 우회 경로도 안전한지 검사
            List<AIPredictedCell> allDangerCells = findDangerCellsNearRoute(request);
            if (GeoUtils.isRouteSafe(bypassRoute.getPath(), allDangerCells)) {
                log.info("우회 성공! 방향 {} ({}): {}", i+1, getDirection(i), passList);
                return bypassRoute;
            }
        }

        log.warn("모든 우회 실패 - 원본 경로 반환");
        return getOriginalRoute(request);
    }

    // 방향 이름 매핑
    private String getDirection(int index) {
        return switch (index) {
            case 0 -> "북";
            case 1 -> "동";
            case 2 -> "남";
            case 3 -> "서";
            default -> "알수없음";
        };
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
