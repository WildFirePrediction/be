package com.capstone25.WildFirePrediction.dto.request;

import lombok.Data;

@Data
public class RouteRequest {
    private Double startLat;    // 출발지 위도
    private Double startLon;    // 출발지 경도
    private Double endLat;      // 목적지 위도
    private Double endLon;      // 목적지 경도
}
