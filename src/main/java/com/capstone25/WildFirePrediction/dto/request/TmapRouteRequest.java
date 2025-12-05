package com.capstone25.WildFirePrediction.dto.request;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TmapRouteRequest {
    private Double startX;       // 출발지 경도
    private Double startY;       // 출발지 위도
    private Double endX;         // 목적지 경도
    private Double endY;         // 목적지 위도
    private String startName;    // 출발지 이름
    private String endName;      // 목적지 이름
    private String reqCoordType; // 요청 좌표계 (WGS84GEO)
    private String resCoordType; // 응답 좌표계 (WGS84GEO)
    private Integer searchOption; // 경로 탐색 옵션 (0: 추천)
    private String passList;     // 경유지 (없으면 null)
}
