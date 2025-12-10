package com.capstone25.WildFirePrediction.controller;

import com.capstone25.WildFirePrediction.global.ApiResponse;
import com.capstone25.WildFirePrediction.service.EmergencyMessageApiService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/emergency-messages")
@RequiredArgsConstructor
public class EmergencyMessageController {

    private final EmergencyMessageApiService emergencyMessageApiService;

    @PostMapping("/test")
    public ApiResponse<Void> testApi() {
        emergencyMessageApiService.testDisasterMessageApi();
        return ApiResponse.onSuccess(null);
    }
}
