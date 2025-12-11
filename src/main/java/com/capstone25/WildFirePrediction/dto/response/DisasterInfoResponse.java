package com.capstone25.WildFirePrediction.dto.response;

import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class DisasterInfoResponse {

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class WildfireListDto {
        private List<WildfireDto> wildfires;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class WildfireDto {
        private Long id;
        private String frstfrInfoId;
        private LocalDateTime ignitionDateTime;
        private String address;
        private Double x;
        private Double y;
        private String sidoCode;
        private String sigunguCode;
        private Double damageArea;
        private Long damageAmount;
        private LocalDateTime maasObtainedAt;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class EarthquakeListDto {
        private List<EarthquakeDto> earthquakes;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class EarthquakeDto {
        private Long id;
        private String earthquakeNo;       // ERQK_NO
        private String branchNo;          // BRNCH_NO
        private String disasterTypeKind;  // MSTN_TYPE_KND
        private LocalDateTime occurrenceTime;
        private Double latitude;
        private Double longitude;
        private String position;
        private Double scale;
        private Double depthKm;
        private String notificationLevel;
        private String refNo;
        private String refMatter;
        private String modificationMatter;
        private LocalDateTime maasObtainedAt;
    }
}
