package com.capstone25.WildFirePrediction.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class EarthquakeApiResponse {

    private Header header;
    private int pageNo;
    private int numOfRows;
    @JsonProperty("totalCount")
    private int totalCount;
    private List<EarthquakeData> body;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Header {
        private String resultCode;
        private String resultMsg;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class EarthquakeData {
        @JsonProperty("BRNCH_NO")
        private String branchNo;                 // 지점번호

        @JsonProperty("MSTN_TYPE_KND")
        private String disasterTypeKind;         // 재난유형종류

        @JsonProperty("PRSNTN_TM")
        private String presentationTimeStr;      // 발표시각

        @JsonProperty("REF_NO")
        private String refNo;                    // 참고번호

        @JsonProperty("INPT_TM")
        private String inputTimeStr;             // 입력시각

        @JsonProperty("NTFCTN_LVL")
        private String notificationLevel;        // 통보레벨

        @JsonProperty("HUR")
        private String hour;                     // 시

        @JsonProperty("LAT")
        private String latitude;                 // 위도

        @JsonProperty("LOT")
        private String longitude;                // 경도

        @JsonProperty("PSTN")
        private String position;                 // 위치

        @JsonProperty("SCL")
        private String scale;                    // 규모

        @JsonProperty("REF_MTTR")
        private String refMatter;                // 참고사항

        @JsonProperty("MDFCN_MTTR")
        private String modificationMatter;       // 수정사항

        @JsonProperty("MAAS_OBNT_DT")
        private String maasObtainDateStr;        // 행정안전부입수일시

        @JsonProperty("CRT_SN")
        private String createSerialNo;           // 생성일련번호

        @JsonProperty("OCRN_DPTH")
        private String occurrenceDepth;          // 발생깊이

        @JsonProperty("DTL_INFO_URI")
        private String detailInfoUri;            // 상세정보URI

        @JsonProperty("ERQK_NO")
        private String earthquakeNo;             // 지진번호

        @JsonProperty("INTENSITY_DST_CODE")
        private String intensityDistCode;        // INTENSITY_DST_CODE
    }
}