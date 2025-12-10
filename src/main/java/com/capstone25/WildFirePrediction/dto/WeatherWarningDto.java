package com.capstone25.WildFirePrediction.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class WeatherWarningDto {

    @JsonProperty("TTL")
    private String title;  // 특보 제목 (예: 대설주의보 해제)

    @JsonProperty("RLVT_ZONE")
    private String relevantZone; // 대상 지역 설명

    @JsonProperty("SPNE_FRMNT_PRCON_TM")
    private String baseDate; // 특보 기준일 (예: 2023-12-12)

    @JsonProperty("PRSNTN_TM")
    private String presentationTime; // 발표 시각 (YYYYMMDDHHmm)

    @JsonProperty("SPNE_FRMNT_TM_TXT")
    private String timeText; // 발표/해제 시각 문장

    @JsonProperty("SPNE_FRMNT_PRCON_CN")
    private String currentAlertContent; // 현재 발효 중인 특보 내용

    @JsonProperty("RSRV_SPNE_PRSNTN_PRCON_")
    private String reservedAlertContent; // 예비 특보 내용

    @JsonProperty("SPNE_PRSNTN_CD")
    private String presentationCode; // 발표 코드

    @JsonProperty("MAAS_OBNT_DT")
    private String obtainedDate; // 데이터 취득일

    @JsonProperty("REF_MTTR")
    private String referenceMatter; // 참고사항

    @JsonProperty("PRSNTN_CN")
    private String presentationContent; // 발표 상세 내용

    @JsonProperty("FCAST")
    private String forecaster; // 예보관 이니셜
}
