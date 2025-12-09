package com.capstone25.WildFirePrediction.service;

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

            // 1. 해당 반경에 대피소 존재 여부 확인 (성능 최적화)
            long count = shelterRepository.countNearbyShelters(lat, lon, radiusKm);
            if (count > 0) {
                log.info("{}km 반경 내 {}개 대피소 발견", radiusKm, count);

                // 2. 실제 데이터 조회
                List<Object[]> results = shelterRepository.findNearbyShelters(lat, lon, radiusKm);
                List<ShelterResponse> shelters = convertToResponse(results);

                log.info("{}km 반경 내 {}개 대피소 반환", radiusKm, shelters.size());
                return shelters;
            }
        }

        log.warn("10km 반경 내 대피소 없음");
        return new ArrayList<>();  // 아무것도 없음
    }

    // Object[] -> ShelterResponse 변환
    private List<ShelterResponse> convertToResponse(List<Object[]> results) {
        return results.stream()
                .map(row -> ShelterResponse.builder()
                        .facilityName((String) row[1])      // facility_name
                        .roadAddress((String) row[2])       // road_address
                        .latitude((Double) row[3])          // latitude
                        .longitude((Double) row[4])        // longitude
                        .shelterTypeName((String) row[5])   // shelter_type_name
                        .distanceKm((Double) row[6])        // distance_km
                        .build()
                )
                .toList();
    }
}
