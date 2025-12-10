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
public class EmergencyMessageDto {

    @JsonProperty("MSG_CN")
    private String messageContent;   // 문자 내용

    @JsonProperty("RCPTN_RGN_NM")
    private String regionName;      // 수신 지역명

    @JsonProperty("CRT_DT")
    private String createdAt;       // 생성 시각 (예: 2023/09/19 12:22:17)

    @JsonProperty("REG_YMD")
    private String regDate;         // 등록일 (예: 2023-09-19)

    @JsonProperty("EMRG_STEP_NM")
    private String stepName;        // 단계 (안전안내, 긴급 등)

    @JsonProperty("SN")
    private Long serialNumber;      // 일련번호 (PK 후보)

    @JsonProperty("DST_SE_NM")
    private String disasterTypeName; // 재난 구분 (기타, 교통통제, 산불 등)

    @JsonProperty("MDFCN_YMD")
    private String modifiedDate;    // 수정일
}
