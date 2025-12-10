package com.capstone25.WildFirePrediction.util;

import com.capstone25.WildFirePrediction.domain.AIPredictedCell;
import java.util.List;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class GeoUtils {

    private static final double EARTH_RADIUS_KM = 6371.0;
    private static final double SAFE_DISTANCE_KM = 0.75;  // 안전 범위 - 750m 보수적

    // Haversine 거리 계산 (km)
    public static double haversine(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_KM * c;
    }

    // 경로 안전 여부 검사
    public static boolean isRouteSafe(List<List<Double>> path, List<AIPredictedCell> cells) {
        return path.stream().noneMatch(point ->
                cells.stream().anyMatch(cell ->
                        haversine(point.get(1), point.get(0), cell.getLatitude(), cell.getLongitude())
                                <= SAFE_DISTANCE_KM
                )
        );
    }
}
