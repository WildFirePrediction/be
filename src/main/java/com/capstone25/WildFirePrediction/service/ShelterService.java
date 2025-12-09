package com.capstone25.WildFirePrediction.service;

import com.capstone25.WildFirePrediction.domain.Shelter;
import com.capstone25.WildFirePrediction.dto.response.ShelterApiResponse;
import com.capstone25.WildFirePrediction.global.code.status.ErrorStatus;
import com.capstone25.WildFirePrediction.global.exception.handler.ExceptionHandler;
import com.capstone25.WildFirePrediction.repository.ShelterRepository;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ShelterService {

    private final ShelterApiService shelterApiService;
    private final ShelterRepository shelterRepository;

    // 대피소 데이터 DB 저장
    public void loadAllShelterData() {
        log.info("대피소 데이터 전체 로드 시작");

        try {
            // 1. 첫 번째 페이지로 총 데이터 수 확인
            int maxPages = 50;  // 임의 설정값
            log.info("최대 {}페이지까지 로드 시작", maxPages);

            int savedCount = 0;
            int skippedCount = 0;

            // 2. 모든 페이지 순회
            for (int pageNo = 1; pageNo <= maxPages; pageNo++) {
                List<ShelterApiResponse.ShelterData> shelterDataList = shelterApiService.fetchShelterPage(pageNo);

                if (shelterDataList == null || shelterDataList.isEmpty()) {
                    log.info("페이지 {}에서 데이터가 없음. 로드 종료", pageNo);
                    break;  // 빈 페이지 나오면 종료
                }

                // 3. DTO -> Entity 변환 및 중복 체크 후 저장
                List<Shelter> sheltersToSave = new ArrayList<>();
                for (ShelterApiResponse.ShelterData data : shelterDataList) {
                    try {
                        Shelter shelter = convertToEntity(data);

                        // 중복 체크 (managementNumber 기준)
                        if (shelterRepository.existsByManagementNumber(shelter.getManagementNumber())) {
                            skippedCount++;
                            log.debug("중복 데이터 스킵: {}", shelter.getManagementNumber());
                            continue;
                        }

                        sheltersToSave.add(shelter);
                    } catch (Exception e) {
                        log.error("대피소 데이터 변환 실패: {}", data.getManagementNumber(), e);
                    }
                }

                // 4. 배치 저장 (성능 최적화)
                if (!sheltersToSave.isEmpty()) {
                    shelterRepository.saveAll(sheltersToSave);
                    savedCount += sheltersToSave.size();
                    log.info("페이지 {} 저장 완료: {}건 (총 저장: {})", pageNo, sheltersToSave.size(), savedCount);
                }
            }
            log.info("대피소 데이터 로드 완료 - 저장: {}, 중복 스킵: {}", savedCount, skippedCount);
        } catch (Exception e) {
            log.error("대피소 데이터 로드 실패", e);
            throw new ExceptionHandler(ErrorStatus.SHELTER_DATA_LOAD_FAILED);
        }
    }

    // API 응답 DTO -> Entity 변환
    private Shelter convertToEntity(ShelterApiResponse.ShelterData data) {
        try {
            // String → Double 변환 (유효성 검사 포함)
            Double latitude = parseCoordinate(data.getLatitude());
            Double longitude = parseCoordinate(data.getLongitude());

            // 위도 범위 검증 (-90 ~ 90)
            if (latitude < -90.0 || latitude > 90.0) {
                throw new IllegalArgumentException("잘못된 위도 값: " + latitude);
            }

            // 경도 범위 검증 (-180 ~ 180)
            if (longitude < -180.0 || longitude > 180.0) {
                throw new IllegalArgumentException("잘못된 경도 값: " + longitude);
            }

            return Shelter.builder()
                    .managementNumber(data.getManagementNumber())
                    .facilityName(data.getFacilityName())
                    .roadAddress(data.getRoadAddress())
                    .latitude(latitude)
                    .longitude(longitude)
                    .shelterTypeCode(data.getShelterTypeCode())
                    .shelterTypeName(data.getShelterTypeName())
                    .build();
        } catch (IllegalArgumentException e) {
            throw new ExceptionHandler(ErrorStatus.SHELTER_API_RESPONSE_INVALID);
        }
    }

    // 좌표값 파싱
    private Double parseCoordinate(String coordinateStr) {
        if (coordinateStr == null || coordinateStr.trim().isEmpty()) {
            throw new IllegalArgumentException("좌표값이 비어있습니다: " + coordinateStr);
        }

        try {
            return Double.parseDouble(coordinateStr.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("유효하지 않은 좌표 형식: " + coordinateStr, e);
        }
    }
}
