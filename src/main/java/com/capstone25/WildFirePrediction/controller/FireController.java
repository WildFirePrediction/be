package com.capstone25.WildFirePrediction.controller;

import com.capstone25.WildFirePrediction.dto.request.AIPredictionRequest.FirePredictionRequestDto;
import com.capstone25.WildFirePrediction.global.ApiResponse;
import com.capstone25.WildFirePrediction.service.AIPredictionService;
import io.swagger.v3.oas.annotations.Operation;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/fires")
@RequiredArgsConstructor
public class FireController {

    private final AIPredictionService aiPredictionService;

    @GetMapping("/active")
    @Operation(summary = "진행 중 화재 예측 조회",
            description = "지도 초기 렌더링용. AI Request JSON 형식 그대로 반환")
    public ApiResponse<List<FirePredictionRequestDto>> getActiveFires() {

        log.info("진행 중 화재 예측 조회 요청");

        List<FirePredictionRequestDto> activeFires = aiPredictionService.getActiveFirePredictionsAsRequestDto();

        log.info("진행 중 화재 예측 조회 완료 - 개수: {}", activeFires.size());

        return ApiResponse.onSuccess(activeFires);
    }
}
