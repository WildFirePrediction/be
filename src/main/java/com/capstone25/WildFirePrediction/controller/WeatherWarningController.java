package com.capstone25.WildFirePrediction.controller;

import com.capstone25.WildFirePrediction.global.ApiResponse;
import com.capstone25.WildFirePrediction.service.WeatherWarningService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/weather-warnings")
@RequiredArgsConstructor
public class WeatherWarningController {

    private final WeatherWarningService weatherWarningService;

    @GetMapping("/raw")
    @Operation(summary = "기상특보 통보문 원시 데이터 조회 (테스트용)",
            description = "입력한 날짜(yyyyMMdd)를 기준으로 기상청 특보통보문 API를 호출하고, " +
                    "응답 JSON을 그대로 반환합니다. 날짜를 비우면 오늘 날짜 기준으로 조회하며, DB에는 저장하지 않습니다.")
    public ApiResponse<String> loadWeatherWarningRaw(@RequestParam(required = false) String date) {
        String rawJson = weatherWarningService.loadWeatherWarningRaw(date);
        return ApiResponse.onSuccess(rawJson);
    }

    @PostMapping("/load")
    @Operation(summary = "기상특보 통보문 수동 동기화",
            description = "입력한 날짜(yyyyMMdd)를 기준으로 기상특보 통보문을 조회하여 DB에 저장합니다. " +
                    "날짜를 비우면 오늘 날짜 기준으로 동기화하며, 중복 데이터는 건너뜁니다.")
    public ApiResponse<String> syncWeatherWarnings(@RequestParam(required = false) String date) {
        if (date == null || date.isBlank()) {
            weatherWarningService.loadTodaysWeatherWarnings();
            return ApiResponse.onSuccess("오늘자 기상특보 동기화를 수행했습니다.");
        } else {
            weatherWarningService.loadWeatherWarningsByDate(date.trim());
            return ApiResponse.onSuccess(date.trim() + " 기준 기상특보 동기화를 수행했습니다.");
        }
    }
}
