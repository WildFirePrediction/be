package com.capstone25.WildFirePrediction.controller;

import com.capstone25.WildFirePrediction.dto.response.NearbyShelterResult;
import com.capstone25.WildFirePrediction.global.ApiResponse;
import com.capstone25.WildFirePrediction.service.ShelterQueryService;
import com.capstone25.WildFirePrediction.service.ShelterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/shelters")
public class ShelterController {

    private final ShelterService shelterService;
    private final ShelterQueryService shelterQueryService;

    // 대피소 데이터 전체를 API에서 가져와 DB에 저장 (서버용, 최초 1회 실행)
    @PostMapping("/load")
    @Operation(summary = "대피소 데이터 전체 로드 (서버용)",
        description = "통합대피소 API에서 대피소 데이터를 모두 가져와 DB에 저장합니다. <br>"
                + "서버용 API입니다. DB가 비었을 때 최초 1회 실행합니다.")
    public ApiResponse<Void> loadAllShelterData() {
        log.info("대피소 데이터 로드 요청 수신");

        shelterService.loadAllShelterData();

        return ApiResponse.onSuccess(null);
    }

    // 현재 위치 기준 근처 대피소 검색 (동적 반경)
    @GetMapping("/nearby")
    @Operation(summary = "근처 대피소 검색",
            description = "현재 위치 기준 3→5→7→10km 순차 검색. <br>"
                    + "가장 가까운 반경에서 결과 반환 (최대 100개)"
    )
    public ApiResponse<NearbyShelterResult> findNearbyShelters(
            @Parameter(description = "현재 위도", example = "37.5665") @RequestParam double lat,
            @Parameter(description = "현재 경도", example = "126.9780") @RequestParam double lon,
            @Parameter(description = "페이지 번호 (0부터)", example = "0") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지당 개수", example = "10") @RequestParam(defaultValue = "10") int size
    ) {
        log.info("대피소 검색 요청 - 위치: ({}, {})", lat, lon);

        NearbyShelterResult shelters = shelterQueryService.findNearbyShelters(lat, lon, page, size);
        return ApiResponse.onSuccess(shelters);
    }
}
