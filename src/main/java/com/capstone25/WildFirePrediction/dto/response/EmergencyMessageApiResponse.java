package com.capstone25.WildFirePrediction.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class EmergencyMessageApiResponse {

    private Header header;
    private int pageNo;
    private int numOfRows;
    @JsonProperty("totalCount")
    private int totalCount;
    private List<EmergencyMessageData> body;

    // API 응답 Header
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Header {
        private String resultCode;
        private String resultMsg;
    }

    // API 응답 본문 데이터
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class EmergencyMessageData {
        @JsonProperty("SN")
        private String serialNumber;           // 일련번호

        @JsonProperty("CRT_DT")
        private String createdAtStr;           // 생성일시 (문자열)

        @JsonProperty("MSG_CN")
        private String messageContent;         // 메시지내용

        @JsonProperty("RCPTN_RGN_NM")
        private String regionName;             // 수신지역명

        @JsonProperty("EMRG_STEP_NM")
        private String stepName;               // 긴급단계명

        @JsonProperty("DST_SE_NM")
        private String disasterTypeName;       // 재해구분명

        @JsonProperty("REG_YMD")
        private String regDateStr;             // 등록일자

        @JsonProperty("MDFCN_YMD")
        private String mdfcnYmdStr;            // 수정일자
    }
}
