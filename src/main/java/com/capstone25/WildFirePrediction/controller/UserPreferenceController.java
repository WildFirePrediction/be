package com.capstone25.WildFirePrediction.controller;

import com.capstone25.WildFirePrediction.domain.UserRegionPreference;
import com.capstone25.WildFirePrediction.dto.response.RegionResponse;
import com.capstone25.WildFirePrediction.global.ApiResponse;
import com.capstone25.WildFirePrediction.service.UserPreferenceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/user-preferences")
public class UserPreferenceController {

    private final UserPreferenceService userPreferenceService;

    @PostMapping
    @Operation(summary = "유저 선호지역 설정",
            description = "최대 3개까지 설정 가능<br>"
                    + "UUID와 함께, 선호지역 ID 리스트를 요청 바디로 전달 (ex. [1, 2, 3])<br>"
                    + "ID는 /regions/by-sido-sigungu 에서 확인가능")
    public ApiResponse<String> setPreferences(
            @RequestHeader("X-DEVICE-UUID")
            @Parameter(description = "기기 UUID", required = true)
            String deviceUuid,
            @RequestBody List<Long> regionIds
    ) {
        userPreferenceService.setPreferences(deviceUuid, regionIds);
        return ApiResponse.onSuccess("선호지역이 저장되었습니다.");
    }

    @GetMapping
    @Operation(summary = "유저 선호지역 조회",
            description = "설정된 선호지역 리스트를 반환<br>"
                    + "UUID를 헤더로 전달")
    public ApiResponse<List<RegionResponse>> getPreferences(
            @RequestHeader("X-DEVICE-UUID")
            @Parameter(description = "기기 UUID", required = true)
            String deviceUuid
    ) {
        List<UserRegionPreference> preferences = userPreferenceService.getPreferences(deviceUuid);

        List<RegionResponse> result = preferences.stream()
                .map(pref -> new RegionResponse(
                        pref.getRegion().getId(),
                        pref.getRegion().getSido(),
                        pref.getRegion().getSigungu(),
                        pref.getRegion().getEupmyeondong()
                ))
                .toList();

        return ApiResponse.onSuccess(result);
    }
}
