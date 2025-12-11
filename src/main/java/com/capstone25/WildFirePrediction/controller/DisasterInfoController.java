package com.capstone25.WildFirePrediction.controller;

import com.capstone25.WildFirePrediction.global.ApiResponse;
import com.capstone25.WildFirePrediction.service.DisasterInfoService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/disaster-info")
@RequiredArgsConstructor
public class DisasterInfoController {

    private final DisasterInfoService disasterInfoService;

    @PostMapping("/wildfire/load-raw")
    @Operation(summary = "원시 산불 재난정보 조회 (테스트용)",
            description = "입력한 날짜(yyyyMMdd)로 공공 API를 호출하여 "
                    + "응답 JSON을 그대로 String으로 반환합니다. DB 저장하지 않습니다.")
    public ApiResponse<String> loadRawWildfireMessages(
            @RequestParam String date
    ) {
        String rawJson = disasterInfoService.loadRawWildfireMessages(date);
        return ApiResponse.onSuccess(rawJson);
    }

    @PostMapping("/earthquake/load-raw")
    @Operation(summary = "원시 지진 재난정보 조회 (테스트용)",
            description = "공공 API를 호출하여 응답 JSON을 그대로 String으로 반환합니다. DB 저장하지 않습니다.")
    public ApiResponse<String> loadRawEarthquakeMessages(
            @RequestParam(defaultValue = "1") int pageNo
    ) {
        String rawJson = disasterInfoService.loadRawEarthquakeMessages(pageNo);
        return ApiResponse.onSuccess(rawJson);
    }
}
