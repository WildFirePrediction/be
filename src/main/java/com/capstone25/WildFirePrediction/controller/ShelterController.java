package com.capstone25.WildFirePrediction.controller;

import com.capstone25.WildFirePrediction.global.ApiResponse;
import com.capstone25.WildFirePrediction.service.ShelterService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/shelters")
public class ShelterController {

    private final ShelterService shelterService;

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
}
