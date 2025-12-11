package com.capstone25.WildFirePrediction.controller;

import com.capstone25.WildFirePrediction.dto.response.DisasterInfoResponse;
import com.capstone25.WildFirePrediction.global.ApiResponse;
import com.capstone25.WildFirePrediction.service.DisasterInfoService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
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

    @PostMapping("/wildfire/load-and-save")
    @Operation(
            summary = "산불 데이터 수집 및 저장 (서버용)",
            description = "입력한 시작일자(yyyyMMdd)를 startDt로 사용해 공공 산불 API를 호출하고, " +
                    "FRSTFR_INFO_ID 기준으로 중복을 제외한 데이터만 DB에 저장합니다."
    )
    public ApiResponse<String> loadAndSaveWildfires(@RequestParam String date) {
        String resultJson = disasterInfoService.loadAndSaveWildfiresByDate(date);
        return ApiResponse.onSuccess(resultJson);
    }

    @GetMapping("/wildfire")
    @Operation(summary = "저장된 산불 재난정보 조회",
            description = "DB에 저장된 산불 재난정보를 전체 조회합니다.")
    public ApiResponse<DisasterInfoResponse.WildfireListDto> getAllWildfires() {
        DisasterInfoResponse.WildfireListDto result = disasterInfoService.getAllWildfires();
        return ApiResponse.onSuccess(result);
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

    @PostMapping("/earthquake/load-and-save-korea")
    @Operation(
            summary = "지진 데이터 수집 및 저장 (한국만, 최신 11페이지)",
            description = "지진 공공 API를 pageNo=11만 조회하고, " +
                    "위도/경도가 한반도 주변(한국)인 데이터만 ERQK_NO 기준 중복 제외 후 DB에 저장합니다."
    )
    public ApiResponse<String> loadAndSaveEarthquakesKoreaOnly() {
        String resultJson = disasterInfoService.loadAndSaveRecentEarthquakesKoreaOnly();
        return ApiResponse.onSuccess(resultJson);
    }

    @GetMapping("/earthquake")
    @Operation(summary = "저장된 지진 재난정보 조회",
            description = "DB에 저장된 지진 재난정보를 전체 조회합니다.")
    public ApiResponse<DisasterInfoResponse.EarthquakeListDto> getAllEarthquakes() {
        DisasterInfoResponse.EarthquakeListDto result = disasterInfoService.getAllEarthquakes();
        return ApiResponse.onSuccess(result);
    }
}
