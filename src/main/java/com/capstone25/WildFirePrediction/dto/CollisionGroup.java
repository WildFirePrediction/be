package com.capstone25.WildFirePrediction.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CollisionGroup {
    private List<CollisionPoint> collisionPoints;
    private CollisionPoint representativePoint;  // 그룹 대표 충돌점 (중앙값)

    // 그룹의 경로상 시작 인덱스 (우선순위 계산용)
    private int startPathIndex;

    // 그룹의 길이 (미터)
    private double groupLength;
}
