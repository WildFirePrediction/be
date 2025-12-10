package com.capstone25.WildFirePrediction.controller;

import com.capstone25.WildFirePrediction.dto.request.AIPredictionRequest.FirePredictionRequestDto;
import com.capstone25.WildFirePrediction.global.ApiResponse;
import com.capstone25.WildFirePrediction.service.AIPredictionService;
import com.capstone25.WildFirePrediction.sse.FireSseEmitterRepository;
import io.swagger.v3.oas.annotations.Operation;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Slf4j
@RestController
@RequestMapping("/fires")
@RequiredArgsConstructor
public class FireController {

    private final AIPredictionService aiPredictionService;
    private final FireSseEmitterRepository emitterRepository;

    private static final long DEFAULT_TIMEOUT = 60L * 60L * 1000L; // 1시간

    @GetMapping("/active")
    @Operation(summary = "진행 중 화재 예측 조회",
            description = "지도 초기 렌더링용. AI Request JSON 형식 그대로 반환")
    public ApiResponse<List<FirePredictionRequestDto>> getActiveFires() {

        log.info("진행 중 화재 예측 조회 요청");

        List<FirePredictionRequestDto> activeFires = aiPredictionService.getActiveFirePredictionsAsRequestDto();

        log.info("진행 중 화재 예측 조회 완료 - 개수: {}", activeFires.size());

        return ApiResponse.onSuccess(activeFires);
    }

    @GetMapping(value = "/sse-stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "산불 예측 실시간 SSE 스트림",
            description = "AI에서 온 예측/종료 이벤트를 그대로 push")
    public SseEmitter subscribeFires() {

        String id = UUID.randomUUID().toString();
        log.info("SSE 구독 시작 - id: {}", id);

        SseEmitter emitter = emitterRepository.add(id, DEFAULT_TIMEOUT);

        // 연결 확인용 더미 이벤트 전송
        try {
            emitter.send(SseEmitter.event().name("connect").data("SSE connected"));
        } catch (Exception e) {
            log.warn("SSE 연결 확인 이벤트 전송 실패 - id: {}, error: {}", id, e.getMessage());
        }

        return emitter;
    }
}
