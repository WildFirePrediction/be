package com.capstone25.WildFirePrediction.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShelterResponse {
    private String facilityName;     // 시설명
    private String roadAddress;      // 도로명주소
    private Double latitude;         // 위도
    private Double longitude;        // 경도
    private String shelterTypeName;  // 대피소구분명 (지진옥외대피장소 등)
    private Integer distanceM;       // 현재 위치로부터 거리(m, 반올림)
}
