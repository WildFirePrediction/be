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
import lombok.extern.slf4j.Slf4j;

@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class GeoUtils {

    private static final double EARTH_RADIUS_KM = 6371.0;
    private static final double COLLISION_DISTANCE_KM = 0.265;  // 375m 셀 대각선/2 ≈ 265m
    private static final double GROUPING_DISTANCE_KM = 0.05;  // 50m 이내 연속 충돌은 같은 그룹
    private static final double COORDINATE_EQUALITY_TOLERANCE = 0.0001; // 위도/경도 비교 허용 오차
    private static final int TMAP_MAX_WAYPOINTS = 5;    // TMAP 최대 경유지 수
    private static final double KM_PER_DEGREE_APPROX = 111.32;  // 위도 1도당 약 111.32km

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

            if (distance <= GROUPING_DISTANCE_KM) {  // 50m 이내
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

        // 못 찾은 경우 보정: 경로의 중간 정도로 넣어 우선순위 왜곡 최소화
        if (startIndex == -1 && !path.isEmpty()) {
            startIndex = path.size() / 2;
        }

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
            if (Math.abs(pathPoint.get(0) - point.getLongitude()) < COORDINATE_EQUALITY_TOLERANCE &&
                    Math.abs(pathPoint.get(1) - point.getLatitude()) < COORDINATE_EQUALITY_TOLERANCE) {
                return i;
            }
        }
        return -1;  // 못 찾음
    }

    // 충돌 그룹들로부터 tmap passList 문자열 생성 (최대 5개 경유지)
    public static String generatePassList(
            List<CollisionGroup> groups,
            double startLon, double startLat,
            double endLon, double endLat,
            List<AIPredictedCell> dangerCells,
            double distanceKm
    ) {
        if (groups.isEmpty()) return null;

        log.info("generatePassList 호출 - 그룹 수: {}", groups.size());

        // 출발지에서 가까운 순으로 정렬 (경로상 인덱스 기준)
        List<CollisionGroup> sortedGroups = groups.stream()
                .sorted(Comparator.comparingInt(CollisionGroup::getStartPathIndex))
                .limit(TMAP_MAX_WAYPOINTS)  // TMAP 최대 5개 제한
                .collect(Collectors.toList());

        sortedGroups.forEach(g -> {
            CollisionPoint rep = g.getRepresentativePoint();
            log.info("우회 대상 그룹 - startIndex: {}, rep(lon,lat): {},{}",
                    g.getStartPathIndex(), rep.getLongitude(), rep.getLatitude());
        });

        List<String> waypoints = new ArrayList<>();
        Double lastLon = null;
        Double lastLat = null;

        for (CollisionGroup group : sortedGroups) {
            CollisionPoint repPoint = group.getRepresentativePoint();
            AIPredictedCell cell = repPoint.getNearestCell();

            // 출발->목적지 방향 벡터 계산
            double[] direction = calculateDirection(startLon, startLat, endLon, endLat);

            // 셀 중심에서 좌/우측 500m 우회점 계산 (더 안전한 쪽 선택)
            double[] bypassPoint = GeoUtils.calculateSafeBypassPoint(
                    cell,
                    direction,
                    distanceKm,
                    dangerCells       // 주변 모든 위험 셀 리스트
            );

            double lon = bypassPoint[0];
            double lat = bypassPoint[1];

            log.info("우회 후보 - cell(lon,lat): {},{} -> bypass(lon,lat): {},{}",
                    cell.getLongitude(), cell.getLatitude(), lon, lat);

            // 직전 경유지와 거의 같은 좌표면 스킵 (0.0001도 ≒ 10m)
            if (lastLon != null && Math.abs(lastLon - lon) < COORDINATE_EQUALITY_TOLERANCE && Math.abs(lastLat - lat) < COORDINATE_EQUALITY_TOLERANCE) {
                log.info("직전 경유지와 거의 동일하여 스킵: {},{}", lon, lat);
                continue;
            }

            waypoints.add(String.format("%.6f,%.6f", bypassPoint[0], bypassPoint[1]));
            lastLon = lon;
            lastLat = lat;

            if (waypoints.size() >= 5) break; // 안전장치
        }

        log.info("최종 passList waypoints {}개: {}", waypoints.size(), waypoints);
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
    private static double[] calculateBypassPointOneSide(double cellLon, double cellLat, double[] direction, double distanceKm, int side) {
        // side: +1 = 좌, -1 = 우
        double perpLon = -direction[1] * side;
        double perpLat =  direction[0] * side;

        // 거리 변환 (km → 도)
        double lonOffset = (distanceKm / KM_PER_DEGREE_APPROX) * Math.cos(Math.toRadians(cellLat));  // 경도 1도=111km
        double latOffset = distanceKm / KM_PER_DEGREE_APPROX;  // 위도 1도=111km

        // 좌측 우회점 계산
        double bypassLon = cellLon + perpLon * lonOffset;
        double bypassLat = cellLat + perpLat * latOffset;

        return new double[]{bypassLon, bypassLat};
    }

    // 셀 중심에서 좌/우측으로 수직 우회점 계산 후 더 안전한 쪽 선택
    public static double[] calculateSafeBypassPoint(
            AIPredictedCell cell,
            double[] direction,
            double distanceKm,
            List<AIPredictedCell> dangerCells
    ) {
        double cellLon = cell.getLongitude();
        double cellLat = cell.getLatitude();

        // 좌/우 후보 우회점 계산
        double[] leftPoint  = calculateBypassPointOneSide(cellLon, cellLat, direction, distanceKm, +1);
        double[] rightPoint = calculateBypassPointOneSide(cellLon, cellLat, direction, distanceKm, -1);

        // 각 후보에 대해 가장 가까운 위험 셀까지 거리 계산
        double leftMinDistKm = minDistanceToCells(leftPoint[1], leftPoint[0], dangerCells);   // lat, lon 순
        double rightMinDistKm = minDistanceToCells(rightPoint[1], rightPoint[0], dangerCells);

        log.info("좌/우 우회 후보 비교 - cell: {},{}  left: {},{} (minDistKm={}), right: {},{} (minDistKm={})",
                cellLon, cellLat,
                leftPoint[0], leftPoint[1], leftMinDistKm,
                rightPoint[0], rightPoint[1], rightMinDistKm
        );

        // 더 멀리 떨어진 쪽(위험 셀에서 더 먼 쪽)을 선택
        if (leftMinDistKm >= rightMinDistKm) {
            return leftPoint;
        } else {
            return rightPoint;
        }
    }

    // 특정 좌표에서 가장 가까운 위험 셀까지 거리(km)
    private static double minDistanceToCells(double lat, double lon, List<AIPredictedCell> cells) {
        return cells.stream()
                .mapToDouble(cell -> haversine(lat, lon, cell.getLatitude(), cell.getLongitude()))
                .min()
                .orElse(Double.MAX_VALUE);
    }
}
