package com.capstone25.WildFirePrediction.dto.response;

import com.capstone25.WildFirePrediction.domain.EmergencyMessage;
import com.capstone25.WildFirePrediction.domain.WeatherWarning;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;

public class RegionResponse {

    @Getter
    @AllArgsConstructor
    public static class RegionResponseDto {
        private Long id;
        private String sido;
        private String sigungu;
        private String eupmyeondong;
    }

    @Data
    @Builder
    public static class RegionDisasterDto {
        private RegionResponseDto region;
        private List<EmergencyMessage> emergencyMessages;
        private List<WeatherWarning> weatherWarnings;
    }
}
