package com.capstone25.WildFirePrediction.controller;

import com.capstone25.WildFirePrediction.global.ApiResponse;
import com.capstone25.WildFirePrediction.service.EmergencyMessageService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/emergency-messages")
@RequiredArgsConstructor
public class EmergencyMessageController {

    private final EmergencyMessageService emergencyMessageService;

    @PostMapping("/load-today")
    @Operation(summary = "오늘 재난문자 데이터 수집 (서버용)",
            description = "오늘 날짜 기준으로 공공 API에서 재난문자 데이터를 조회하여, " +
                    "이미 저장된 일련번호를 제외한 신규 데이터만 DB에 저장합니다. <br>"
                    + "별도 실행 없이도 스케줄러를 통해 매일 10분마다 자동 실행됩니다.")
    public ApiResponse<String> loadTodaysMessages() {
        emergencyMessageService.loadTodaysEmergencyMessages();
        return ApiResponse.onSuccess("오늘 재난문자 수집 및 저장 완료");
    }

    @PostMapping("/load-by-date")
    @Operation(summary = "특정 일자 재난문자 데이터 수집 (서버용)",
            description = "요청한 날짜(yyyyMMdd)를 crtDt로 사용해 재난문자 데이터를 조회하고, " +
                    "이미 저장된 일련번호를 제외한 신규 데이터만 DB에 저장합니다.")
    public ApiResponse<String> loadMessagesByDate(String date) {
        // date 파라미터가 null/빈값이면 내부에서 오늘 날짜로 처리
        emergencyMessageService.loadEmergencyMessagesByDate(date);
        return ApiResponse.onSuccess("재난문자 수집 및 저장 완료 (crtDt=" + date + ")");
    }

    @PostMapping("/load-raw")
    @Operation(summary = "원시 재난문자 데이터 조회 (테스트용)",
            description = "입력한 날짜(yyyyMMdd)로 공공 API를 호출하여 " +
                    "응답 JSON을 그대로 String으로 반환합니다. DB 저장하지 않습니다.")
    public ApiResponse<String> loadRawMessages(@RequestParam String date) {
        String rawJson = emergencyMessageService.loadRawEmergencyMessages(date);
        return ApiResponse.onSuccess(rawJson);
    }
}
