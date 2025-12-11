package com.capstone25.WildFirePrediction.dto;

import com.capstone25.WildFirePrediction.domain.AIPredictedCell;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CollisionPoint {
    private double longitude;      // 충돌한 경로 좌표 경도
    private double latitude;       // 충돌한 경로 좌표 위도
    private AIPredictedCell nearestCell;  // 가장 가까운 위험 셀
    private double distanceToCell; // 셀 중심까지 거리(km)
}
