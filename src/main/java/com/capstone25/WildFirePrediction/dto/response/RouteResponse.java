package com.capstone25.WildFirePrediction.dto.response;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RouteResponse {

    private Integer totalDistance;  // 총 거리 (m)
    private Integer totalTime;      // 총 소요시간 (초)
    private List<List<Double>> path; // 경로 좌표 배열 [[lon, lat], [lon, lat], ...]
    private String message;         // 추가 메시지 (옵션)
}
