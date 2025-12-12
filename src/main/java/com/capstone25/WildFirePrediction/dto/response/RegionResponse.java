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
        private List<WeatherWarningDto> weatherWarnings;
    }

    @Data
    @Builder
    public static class WeatherWarningDto {
        private String branch;
        private String presentationTime;
        private String presentationSerial;
        private String forecasterName;
        private String warningPresentationCode;
        private String title;
        private String relevantZone;
        private String effectiveTimeText;
        private String content;
        private String effectiveStatusTimeRaw;  // 포맷팅된 값
        private String effectiveStatusContent;
        private String reservedWarningStatus;
        private String referenceMatter;
        private String maasObtainedAtRaw;  // 포맷팅된 값
    }
}
