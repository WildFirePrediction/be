package com.capstone25.WildFirePrediction.repository;

import com.capstone25.WildFirePrediction.domain.Shelter;
import com.capstone25.WildFirePrediction.dto.projection.ShelterProjection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ShelterRepository extends JpaRepository<Shelter, Long> {

    // 관리일련번호로 대피소 조회
    Optional<Shelter> findByManagementNumber(String managementNumber);

    // 관리일련번호 존재 여부 확인 (중복 방지)
    boolean existsByManagementNumber(String managementNumber);

    // 위치 기준 반경 내 대피소 검색 (네모박스 + Haversine 하이브리드
    @Query(value = """
    SELECT s.id AS id, 
           s.facility_name AS facilityName, 
           s.road_address AS roadAddress, 
           s.latitude AS latitude, 
           s.longitude AS longitude, 
           s.shelter_type_name AS shelterTypeName,
           (6371 * acos(LEAST(1.0, GREATEST(-1.0, 
               cos(radians(:lat)) * cos(radians(s.latitude)) * 
               cos(radians(s.longitude) - radians(:lon)) + 
               sin(radians(:lat)) * sin(radians(s.latitude))
           )))) AS distanceKm
    FROM shelter s 
    WHERE s.latitude BETWEEN :minLat AND :maxLat 
      AND s.longitude BETWEEN :minLon AND :maxLon
    HAVING distanceKm < :radiusKm
    ORDER BY distanceKm ASC
    LIMIT 100
    """, nativeQuery = true)
    List<ShelterProjection> findNearbyShelters(
            @Param("lat") double lat,
            @Param("lon") double lon,
            @Param("radiusKm") double radiusKm,
            @Param("minLat") double minLat,
            @Param("maxLat") double maxLat,
            @Param("minLon") double minLon,
            @Param("maxLon") double maxLon
    );
}
