package com.capstone25.WildFirePrediction.service;

import com.capstone25.WildFirePrediction.dto.BoundingBox;
import com.capstone25.WildFirePrediction.dto.projection.ShelterProjection;
import com.capstone25.WildFirePrediction.dto.response.NearbyShelterResult;
import com.capstone25.WildFirePrediction.dto.response.ShelterResponse;
import com.capstone25.WildFirePrediction.repository.ShelterRepository;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ShelterQueryService {

    private final ShelterRepository shelterRepository;

    // 검색 반경 단계 (km)
    private static final int[] SEARCH_RADII_KM = {3, 5, 7, 10};
    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_SHELTERS_FIRST_PAGE = 50;  // 첫페이지 최대치

    private static final double KM_PER_DEGREE = 111.0;  // 1도 = 111km

    // 현재 위치 기준 동적 반경 검색 (3 -> 5 -> 7 -> 10km), 페이징 처리
    public NearbyShelterResult findNearbyShelters(double lat, double lon, int page, int size) {
        log.info("대피소 검색 시작 - 위치: ({}, {}) 페이지: {}", lat, lon, page);

        // page, size 검증
        int effectivePage = Math.max(0, page);
        int effectiveSize = Math.max(1, Math.min(50, size));  // 1~50 제한

        // 1. 가장 작은 반경 찾기 (최적화!)
        int optimalRadius = findOptimalRadius(lat, lon);
        log.info("최적 반경 {}km 선택", optimalRadius);

        // 2. 네모박스 + 페이징 쿼리
        BoundingBox bbox = calculateBoundingBox(lat, lon, optimalRadius);
        List<ShelterProjection> projections = shelterRepository.findNearbySheltersPaged(
                lat, lon, optimalRadius,
                bbox.getMinLat(), bbox.getMaxLat(), bbox.getMinLon(), bbox.getMaxLon(),
                effectivePage * effectiveSize,  // OFFSET
                effectiveSize                   // LIMIT
        );

        // 3. 전체 개수 + 페이징 정보
        long totalCount = shelterRepository.countNearbySheltersPaged(
                lat, lon, optimalRadius,
                bbox.getMinLat(), bbox.getMaxLat(), bbox.getMinLon(), bbox.getMaxLon());

        List<ShelterResponse> shelters = projections.stream()
                .map(this::projectionToResponse)
                .toList();

        return NearbyShelterResult.builder()
                .shelters(shelters)
                .radiusKm(optimalRadius)
                .page(effectivePage)
                .size(effectiveSize)
                .totalCount(totalCount)
                .hasMore((effectivePage + 1) * effectiveSize < totalCount)
                .build();
    }

    // 최적 반경 찾기 (하이브리드 검증)
    private int findOptimalRadius(double lat, double lon) {
        for (int radius : SEARCH_RADII_KM) {
            BoundingBox bbox = calculateBoundingBox(lat, lon, radius);
            // 하이브리드 쿼리로 1개만 가져와서 존재 여부 확인 (초고속!)
            Optional<ShelterProjection> first = shelterRepository.findNearbySheltersPaged(
                    lat, lon, radius,
                    bbox.getMinLat(), bbox.getMaxLat(), bbox.getMinLon(), bbox.getMaxLon(),
                    0, 1  // 첫 번째만!
            ).stream().findFirst();

            if (first.isPresent()) {
                log.debug("{}km 반경에 데이터 존재 확인", radius);
                return radius;
            }
        }
        return SEARCH_RADII_KM[SEARCH_RADII_KM.length - 1];
    }

    // 네모박스 계산 (1km 당 약 0.009도)
    private BoundingBox calculateBoundingBox(double lat, double lon, double radiusKm) {
        double deltaDegree = radiusKm / KM_PER_DEGREE;

        double minLat = lat - deltaDegree;
        double maxLat = lat + deltaDegree;
        double lonDegree = deltaDegree / Math.cos(Math.toRadians(lat));  // 위도 보정
        double minLon = lon - lonDegree;
        double maxLon = lon + lonDegree;

        return BoundingBox.builder()
                .minLat(minLat).maxLat(maxLat)
                .minLon(minLon).maxLon(maxLon)
                .build();
    }

    // Projection → Response 변환
    private ShelterResponse projectionToResponse(ShelterProjection projection) {
        // km → m 반올림 (소수점 버림)
        int distanceMeters = (int) Math.floor(projection.getDistanceKm() * 1000);

        return ShelterResponse.builder()
                .facilityName(projection.getFacilityName())
                .roadAddress(projection.getRoadAddress())
                .latitude(projection.getLatitude())
                .longitude(projection.getLongitude())
                .shelterTypeName(projection.getShelterTypeName())
                .distanceM(distanceMeters)
                .build();
    }
}
