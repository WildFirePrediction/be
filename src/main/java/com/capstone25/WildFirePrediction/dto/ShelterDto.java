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
@JsonIgnoreProperties(ignoreUnknown = true) // 추후 추가 필드 무시
public class ShelterDto {
    @JsonProperty("REARE_NM")
    private String facilityName;  // 시설명

    @JsonProperty("RONA_DADDR")
    private String roadAddress;  // 도로명전체주소

    @JsonProperty("LAT")
    private String latitude;     // 위도

    @JsonProperty("LOT")
    private String longitude;    // 경도

    @JsonProperty("SHLT_SE_CD")
    private String shelterTypeCode;  // 대피소구분코드

    @JsonProperty("SHLT_SE_NM")
    private String shelterTypeName;  // 대피소구분명

    @JsonProperty("MNG_SN")
    private String managementNumber; // 관리일련번호
}
