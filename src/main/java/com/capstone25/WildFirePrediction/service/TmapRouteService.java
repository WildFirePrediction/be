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
        return findBypassRoute(request, originalRoute, dangerCells);
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
    private RouteResponse findBypassRoute(RouteRequest request, RouteResponse originalRoute,
                                          List<AIPredictedCell> dangerCells) {
        // 위험 셀 없으면 그냥 원본 반환
        if (dangerCells == null || dangerCells.isEmpty()) {
            log.info("위험 셀이 없어 우회 없이 원본 경로 반환");
            return originalRoute;
        }

        // 위험 박스 계산
        double minLat =  90.0;
        double maxLat = -90.0;
        double minLon =  180.0;
        double maxLon = -180.0;

        for (AIPredictedCell cell : dangerCells) {
            double lat = cell.getLatitude();
            double lon = cell.getLongitude();
            if (lat < minLat) minLat = lat;
            if (lat > maxLat) maxLat = lat;
            if (lon < minLon) minLon = lon;
            if (lon > maxLon) maxLon = lon;
        }

        // 위험 박스에 약간 여유(200m 정도) 추가
        double margin = 0.002; // ≒ 200m
        minLat -= margin;
        maxLat += margin;
        minLon -= margin;
        maxLon += margin;

        double boxCenterLat = (minLat + maxLat) / 2.0;
        double boxCenterLon = (minLon + maxLon) / 2.0;

        // 박스 바깥 경유지 후보들 (약 2km 밖으로)
        double d = 0.02; // 위도/경도 0.02도 ≒ 2km 근처 (서울 위도 기준 대략)

        double[] north = { boxCenterLon, maxLat + d }; // 북쪽 바깥
        double[] south = { boxCenterLon, minLat - d }; // 남쪽 바깥
        double[] east  = { maxLon + d,   boxCenterLat }; // 동쪽 바깥
        double[] west  = { minLon - d,   boxCenterLat }; // 서쪽 바깥

        // 다중 경유지 패턴 정의 (최대 3개 경유지)
        String passList1 = String.format(
                "%.6f,%.6f_%.6f,%.6f_%.6f,%.6f",
                south[0], south[1],
                west[0],  west[1],
                north[0], north[1]
        ); // 남 → 서 → 북 (서남쪽으로 크게 돌아 위로)

        String passList2 = String.format(
                "%.6f,%.6f_%.6f,%.6f_%.6f,%.6f",
                north[0], north[1],
                east[0],  east[1],
                south[0], south[1]
        ); // 북 → 동 → 남

        String passList3 = String.format(
                "%.6f,%.6f_%.6f,%.6f",
                west[0],  west[1],
                south[0], south[1]
        ); // 서 → 남 (2점만)

        String passList4 = String.format(
                "%.6f,%.6f_%.6f,%.6f",
                east[0],  east[1],
                north[0], north[1]
        ); // 동 → 북 (2점만)

        String[] passLists = { passList1, passList2, passList3, passList4 };

        // 패턴별로 Tmap 우회 경로 시도
        for (int i = 0; i < passLists.length; i++) {
            String passList = passLists[i];
            log.info("우회 패턴 {} 시도: passList={}", i + 1, passList);

            TmapApiRequest bypassRequest = TmapApiRequest.builder()
                    .startX(request.getStartLon())
                    .startY(request.getStartLat())
                    .endX(request.getEndLon())
                    .endY(request.getEndLat())
                    .passList(passList)   // 여러 경유지 사용
                    .startName("현재 위치")
                    .endName("대피소")
                    .reqCoordType("WGS84GEO")
                    .resCoordType("WGS84GEO")
                    .searchOption(0)
                    .build();

            TmapApiResponse bypassResponse;
            try {
                bypassResponse = callTmapApi(bypassRequest);
            } catch (Exception e) {
                log.warn("우회 패턴 {} TMAP 호출 실패, 다음 패턴 시도: {}", i + 1, e.getMessage());
                continue;
            }

            RouteResponse bypassRoute = parseRouteResponse(bypassResponse);

            // 우회 경로도 안전한지 검사
            if (GeoUtils.isRouteSafe(bypassRoute.getPath(), dangerCells)) {
                log.info("✅ 우회 성공! 패턴 {} 사용, 총 거리: {}m, 시간: {}분",
                        i + 1, bypassRoute.getTotalDistance(), bypassRoute.getTotalTime());
                return bypassRoute;
            }

            log.info("우회 패턴 {} 경로도 여전히 위험, 다음 패턴 시도", i + 1);
        }


        log.warn("모든 우회 실패 - 원본 경로 반환");
        return originalRoute;
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
