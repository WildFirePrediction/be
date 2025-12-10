package com.capstone25.WildFirePrediction.controller;

import com.capstone25.WildFirePrediction.global.ApiResponse;
import com.capstone25.WildFirePrediction.service.WeatherWarningService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/weather-warnings")
@RequiredArgsConstructor
public class WeatherWarningController {

    private final WeatherWarningService weatherWarningService;

    @PostMapping("/weather-warning/raw")
    @Operation(summary = "기상특보 통보문 원시 데이터 조회 (테스트용)",
            description = "입력한 날짜(yyyyMMdd)를 기준으로 기상청 특보통보문 API를 호출하고, " +
                    "응답 JSON을 그대로 반환합니다. DB에는 저장하지 않습니다.")
    public ApiResponse<String> loadWeatherWarningRaw(@RequestParam String date) {
        String rawJson = weatherWarningService.loadWeatherWarningRaw(date);
        return ApiResponse.onSuccess(rawJson);
    }
}
