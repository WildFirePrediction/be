package com.capstone25.WildFirePrediction.util;

import com.capstone25.WildFirePrediction.domain.AIPredictedCell;
import com.capstone25.WildFirePrediction.dto.CollisionGroup;
import com.capstone25.WildFirePrediction.dto.CollisionPoint;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class GeoUtils {

    private static final double EARTH_RADIUS_KM = 6371.0;
    private static final double SAFE_DISTANCE_KM = 0.75;  // 안전 범위 - 750m 보수적
    private static final double COLLISION_DISTANCE_KM = 0.265;  // 375m 셀 대각선/2 ≈ 265m

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
        return findCollisionPoints(path, cells).isEmpty();
    }

    // 경로에서 실제 충돌 지점들을 찾아 CollisionPoint 리스트로 반환
    public static List<CollisionPoint> findCollisionPoints(List<List<Double>> path, List<AIPredictedCell> cells) {
        return path.stream()
                .map(point -> {
                    double lon = point.get(0);  // [lon, lat] 순서
                    double lat = point.get(1);

                    // 가장 가까운 셀 찾기
                    AIPredictedCell nearestCell = cells.stream()
                            .min(Comparator.comparingDouble(cell ->
                                    haversine(lat, lon, cell.getLatitude(), cell.getLongitude())))
                            .orElse(null);

                    if (nearestCell == null) return null;

                    double distance = haversine(lat, lon, nearestCell.getLatitude(), nearestCell.getLongitude());

                    // 충돌 기준: 265m 이내
                    if (distance <= COLLISION_DISTANCE_KM) {
                        return new CollisionPoint(lon, lat, nearestCell, distance);
                    }
                    return null;
                })
                .filter(point -> point != null)  // 충돌한 점만
                .collect(Collectors.toList());
    }

    // 연속된 충돌점들을 그룹화 (50m 간격 이내는 같은 그룹)
    public static List<CollisionGroup> groupCollisions(List<CollisionPoint> collisions, List<List<Double>> path) {
        if (collisions.isEmpty()) return new ArrayList<>();

        List<CollisionGroup> groups = new ArrayList<>();
        List<CollisionPoint> currentGroup = new ArrayList<>();
        CollisionPoint firstPoint = collisions.get(0);
        currentGroup.add(firstPoint);

        // 경로상 인덱스 계산을 위한 맵
        for (int i = 1; i < collisions.size(); i++) {
            CollisionPoint prev = collisions.get(i - 1);
            CollisionPoint curr = collisions.get(i);

            // 50m 이내 연속 충돌 → 같은 그룹
            double distance = haversine(prev.getLatitude(), prev.getLongitude(),
                    curr.getLatitude(), curr.getLongitude());

            if (distance <= 0.05) {  // 50m 이내
                currentGroup.add(curr);
            } else {
                // 새 그룹 시작
                groups.add(createGroup(currentGroup, path));
                currentGroup = new ArrayList<>();
                currentGroup.add(curr);
            }
        }

        // 마지막 그룹 추가
        if (!currentGroup.isEmpty()) {
            groups.add(createGroup(currentGroup, path));
        }

        return groups;
    }

    private static CollisionGroup createGroup(List<CollisionPoint> points, List<List<Double>> path) {
        if (points.isEmpty()) return null;

        // 대표점: 그룹의 중앙값 (중앙 인덱스)
        CollisionPoint representative = points.get(points.size() / 2);

        // 경로상 시작 인덱스 (첫 번째 점의 경로상 위치)
        int startIndex = findPathIndex(points.get(0), path);

        // 그룹 길이 계산 (첫점~막점 직선거리)
        double groupLength = points.size() > 1 ?
                haversine(points.get(0).getLatitude(), points.get(0).getLongitude(),
                        points.get(points.size()-1).getLatitude(), points.get(points.size()-1).getLongitude()) * 1000 : 0;

        return new CollisionGroup(points, representative, startIndex, groupLength);
    }

    // CollisionPoint가 경로상의 몇 번째 인덱스인지 찾기
    private static int findPathIndex(CollisionPoint point, List<List<Double>> path) {
        for (int i = 0; i < path.size(); i++) {
            List<Double> pathPoint = path.get(i);
            if (Math.abs(pathPoint.get(0) - point.getLongitude()) < 0.0001 &&
                    Math.abs(pathPoint.get(1) - point.getLatitude()) < 0.0001) {
                return i;
            }
        }
        return 0;  // 정확히 일치하지 않으면 0
    }

    // 충돌 그룹들로부터 tmap passList 문자열 생성 (최대 5개 경유지)
    public static String generatePassList(List<CollisionGroup> groups, double startLon, double startLat, double endLon, double endLat) {
        if (groups.isEmpty()) return null;

        // 출발지에서 가까운 순으로 정렬 (경로상 인덱스 기준)
        List<CollisionGroup> sortedGroups = groups.stream()
                .sorted(Comparator.comparingInt(CollisionGroup::getStartPathIndex))
                .limit(5)  // TMAP 최대 5개 제한
                .collect(Collectors.toList());

        List<String> waypoints = new ArrayList<>();

        for (CollisionGroup group : sortedGroups) {
            CollisionPoint repPoint = group.getRepresentativePoint();
            AIPredictedCell cell = repPoint.getNearestCell();

            // 출발->목적지 방향 벡터 계산
            double[] direction = calculateDirection(startLon, startLat, endLon, endLat);

            // 셀 중심에서 좌/우측 500m 우회점 계산 (더 안전한 쪽 선택)
            double[] bypassPoint = calculateBypassPoint(
                    cell.getLongitude(), cell.getLatitude(),
                    direction, 0.005  // 500m (0.005도)
            );

            waypoints.add(String.format("%.6f,%.6f", bypassPoint[0], bypassPoint[1]));
        }

        return String.join("_", waypoints);
    }

    // 출발지 -> 목적지 방향 벡터 계산
    private static double[] calculateDirection(double startLon, double startLat, double endLon, double endLat) {
        double dLon = endLon - startLon;
        double dLat = endLat - startLat;
        double length = Math.sqrt(dLon * dLon + dLat * dLat);

        return new double[]{dLon / length, dLat / length};  // 단위 벡터
    }

    // 셀 중심에서 좌/우측으로 수직 우회점 계산
    private static double[] calculateBypassPoint(double cellLon, double cellLat, double[] direction, double distanceKm) {
        // 수직 벡터 계산: [-direction[1], direction[0]] (좌회전)
        double perpLon = -direction[1];  // 위도 방향 반전
        double perpLat =  direction[0];  // 경도 방향 유지

        // 거리 변환 (km → 도)
        double lonOffset = (distanceKm / 111.32) * Math.cos(Math.toRadians(cellLat));  // 경도 1도=111km
        double latOffset = distanceKm / 111.32;  // 위도 1도=111km

        // 좌측 우회점 계산
        double bypassLon = cellLon + perpLon * lonOffset;
        double bypassLat = cellLat + perpLat * latOffset;

        return new double[]{bypassLon, bypassLat};
    }
}
