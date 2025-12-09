package com.capstone25.WildFirePrediction.service;

import com.capstone25.WildFirePrediction.dto.BoundingBox;
import com.capstone25.WildFirePrediction.dto.projection.ShelterProjection;
import com.capstone25.WildFirePrediction.dto.response.ShelterResponse;
import com.capstone25.WildFirePrediction.repository.ShelterRepository;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ShelterQueryService {

    private final ShelterRepository shelterRepository;

    // 현재 위치 기준 동적 반경 검색 (3 -> 5 -> 7 -> 10km)
    public List<ShelterResponse> findNearbyShelters(double lat, double lon) {
        log.info("대피소 검색 시작 - 위치: ({}, {})", lat, lon);

        int[] radii = {3, 5, 7, 10};    // 동적 반경: 3km → 5km → 7km → 10km

        for (int radiusKm : radii) {
            log.debug("반경 {}km 검색 중...", radiusKm);

            // 1. 네모박스 계산 (인덱스 활용)
            BoundingBox bbox = calculateBoundingBox(lat, lon, radiusKm);

            // 2. 하이브리드 쿼리 실행 (네모박스 + Haversine)
            List<ShelterProjection> projections = shelterRepository.findNearbyShelters(
                    lat, lon, radiusKm,
                    bbox.getMinLat(), bbox.getMaxLat(),
                    bbox.getMinLon(), bbox.getMaxLon()
            );

            if (!projections.isEmpty()) {
                List<ShelterResponse> shelters = projections.stream()
                        .map(this::projectionToResponse)
                        .toList();
                log.info("{}km 반경 내 {}개 대피소 반환 (네모박스 필터링 적용)",
                        radiusKm, shelters.size());
                return shelters;
            }
        }

        log.warn("10km 반경 내 대피소 없음");
        return new ArrayList<>();  // 아무것도 없음
    }

    // 네모박스 계산 (1km 당 약 0.009도)
    private BoundingBox calculateBoundingBox(double lat, double lon, double radiusKm) {
        final double KM_PER_DEGREE = 111.0;  // 1도 = 111km
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
        return ShelterResponse.builder()
                .facilityName(projection.getFacilityName())
                .roadAddress(projection.getRoadAddress())
                .latitude(projection.getLatitude())
                .longitude(projection.getLongitude())
                .shelterTypeName(projection.getShelterTypeName())
                .distanceKm(projection.getDistanceKm())
                .build();
    }
}
