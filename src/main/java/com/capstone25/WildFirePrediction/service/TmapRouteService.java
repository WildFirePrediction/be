package com.capstone25.WildFirePrediction.service;

import com.capstone25.WildFirePrediction.domain.AIPredictedCell;
import com.capstone25.WildFirePrediction.dto.CollisionGroup;
import com.capstone25.WildFirePrediction.dto.CollisionPoint;
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

        // 4. 우회 경로 반환
        RouteResponse safeRoute = findBypassRoute(request, originalRoute, dangerCells);
        log.info("최종 경로 - 거리: {}m, 시간: {}분, 안전여부: {}",
                safeRoute.getTotalDistance(), safeRoute.getTotalTime(),
                safeRoute != originalRoute ? "✅" : "⚠️");
        return safeRoute;
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

    // 정밀 충돌 기반 우회 경로 탐색 (반복 3회 최대)
    private RouteResponse findBypassRoute(
            RouteRequest request, RouteResponse originalRoute, List<AIPredictedCell> dangerCells) {
        // 1. 실제 충돌 지점들 탐지
        List<CollisionPoint> collisions = GeoUtils.findCollisionPoints(
                originalRoute.getPath(), dangerCells);

        if (collisions.isEmpty()) {
            log.info("✅ 충돌 없음, 원본 경로 안전");
            return originalRoute;
        }

        // 2. 충돌 그룹화
        List<CollisionGroup> groups = GeoUtils.groupCollisions(collisions, originalRoute.getPath());
        log.info("충돌 그룹 {}개 생성", groups.size());

        // 3. 반복 우회 시도 (최대 3회)
        for (int iteration = 1; iteration <= 3; iteration++) {
            log.info("=== 우회 반복 {}/3 ===", iteration);

            // 4. passList 생성 (최대 5개 경유지)
            String passList = GeoUtils.generatePassList(
                    groups,
                    request.getStartLon(), request.getStartLat(),
                    request.getEndLon(), request.getEndLat(),
                    dangerCells
            );

            log.info("생성된 passList: {}", passList);

            if (passList == null || passList.isEmpty()) {
                log.warn("passList 생성 실패");
                break;
            }

            // 5. 경유지 포함 TMAP API 호출
            RouteResponse bypassRoute = getRouteWithPassList(request, passList);

            // 6. 안전 검증
            if (GeoUtils.isRouteSafe(bypassRoute.getPath(), dangerCells)) {
                log.info("✅ 우회 성공! 반복 {}회, 경유지 {}, 거리: {}m",
                        iteration, groups.size(), bypassRoute.getTotalDistance());
                return bypassRoute;
            }

            log.warn("반복 {}회 실패 - 재충돌 탐지", iteration);

            // 7. 다음 반복을 위해 충돌 정보 업데이트
            collisions = GeoUtils.findCollisionPoints(bypassRoute.getPath(), dangerCells);
            groups = GeoUtils.groupCollisions(collisions, bypassRoute.getPath());

            // 그룹이 없어졌으면 성공 (드문 경우)
            if (groups.isEmpty()) {
                log.info("✅ 우회 후 충돌 완전 제거!");
                return bypassRoute;
            }
        }

        log.error("❌ 모든 우회 시도 실패 (3회) - 위험 경로 반환");
        return originalRoute;
    }

    // 경유지 포함 TMAP 경로 조회
    private RouteResponse getRouteWithPassList(RouteRequest request, String passList) {
        TmapApiRequest tmapRequest = TmapApiRequest.builder()
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

        TmapApiResponse tmapResponse = callTmapApi(tmapRequest);
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
