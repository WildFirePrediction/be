package com.capstone25.WildFirePrediction.controller;

import com.capstone25.WildFirePrediction.dto.request.AIPredictionRequest.FirePredictionRequestDto;
import com.capstone25.WildFirePrediction.dto.response.AIPredictionResponse;
import com.capstone25.WildFirePrediction.service.AIPredictionService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/ai-predictions")
@RequiredArgsConstructor
public class AIPredictionController {

    private final AIPredictionService aiPredictionService;

    // AI 예측 데이터 수신 엔드포인트
    @PostMapping("")
    @Operation(summary = "AI 산불 확산 예측 데이터 수신",
            description = "AI 서버용 엔드포인트입니다. 스웨거에서 사용 금지")
    public ResponseEntity<AIPredictionResponse> receivePrediction(
            @Valid @RequestBody FirePredictionRequestDto requestDto,
            BindingResult bindingResult) {

        log.info("AI 예측 데이터 수신 - fireId: {}, eventType: {}",
                requestDto.getFireId(), requestDto.getEventType());

        // 1. Validation 에러 체크
        if (bindingResult.hasErrors()) {
            String errorMessage = bindingResult.getAllErrors().get(0).getDefaultMessage();
            log.error("요청 데이터 검증 실패 - fireId: {}, error: {}",
                    requestDto.getFireId(), errorMessage);

            // 검증 실패해도 200 반환 (AI 서버가 재시도하지 않도록)
            return ResponseEntity.ok(AIPredictionResponse.success());
        }

        // 2. 비동기 처리 호출
        aiPredictionService.processAIPrediction(requestDto);

        // 3. 즉시 응답 반환
        log.info("AI 예측 데이터 수신 완료 - fireId: {} (비동기 처리 시작)",
                requestDto.getFireId());

        return ResponseEntity.ok(AIPredictionResponse.success());
    }
}
