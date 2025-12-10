package com.capstone25.WildFirePrediction.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class WeatherWarningApiResponse {

    private Header header;
    private int numOfRows;
    private int pageNo;
    @JsonProperty("totalCount")
    private int totalCount;
    private List<WeatherWarningData> body;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Header {
        private String resultMsg;
        private String resultCode;
        private String errorMsg;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class WeatherWarningData {

        @JsonProperty("BRNCH")
        private Integer branch;                 // 지점

        @JsonProperty("PRSNTN_TM")
        private String presentationTimeStr;     // 발표시각 (yyyyMMddHHmm)

        @JsonProperty("PRSNTN_SN")
        private Long presentationSerial;        // 발표일련번호

        @JsonProperty("FCAST")
        private String forecasterName;          // 예보관

        @JsonProperty("SPNE_PRSNTN_CD")
        private String warningPresentationCode; // 특보발표코드

        @JsonProperty("TTL")
        private String title;                   // 제목

        @JsonProperty("RLVT_ZONE")
        private String relevantZone;            // 해당구역

        @JsonProperty("SPNE_FRMNT_TM_TXT")
        private String effectiveTimeText;       // 특보발효시각텍스트

        @JsonProperty("PRSNTN_CN")
        private String content;                 // 발표내용

        @JsonProperty("SPNE_FRMNT_PRCON_TM")
        private String effectiveStatusTimeRaw;  // 특보발효현황시각 (원문)

        @JsonProperty("SPNE_FRMNT_PRCON_CN")
        private String effectiveStatusContent;  // 특보발효현황내용

        @JsonProperty("RSRV_SPNE_PRSNTN_PRCON_")
        private String reservedWarningStatus;   // 예비특보발표현황

        @JsonProperty("REF_MTTR")
        private String referenceMatter;         // 참고사항

        @JsonProperty("MAAS_OBNT_DT")
        private String maasObtainedAtRaw;       // 행안부 입수일시 (원문)
    }
}
