package com.capstone25.WildFirePrediction.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AIPredictionResponse {

    // 처리 상태 (success / error)
    private String status;

    // 메시지
    private String message;

    // 성공 응답 생성
    public static AIPredictionResponse success() {
        return AIPredictionResponse.builder()
                .status("success")
                .message("Predictions received")
                .build();
    }

    // 에러 응답 생성
    public static AIPredictionResponse error(String message) {
        return AIPredictionResponse.builder()
                .status("error")
                .message(message)
                .build();
    }
}
