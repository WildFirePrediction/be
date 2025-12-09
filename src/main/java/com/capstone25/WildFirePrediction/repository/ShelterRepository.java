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

    // 페이징 쿼리 (OFFSET/LIMIT) - 네이티브 쿼리 사용
    @Query(value = """
    SELECT s.id AS id, s.facility_name AS facilityName, 
           s.road_address AS roadAddress, s.latitude AS latitude, 
           s.longitude AS longitude, s.shelter_type_name AS shelterTypeName,
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
    LIMIT :limit OFFSET :offset
    """, nativeQuery = true)
    List<ShelterProjection> findNearbySheltersPaged(
            @Param("lat") double lat, @Param("lon") double lon, @Param("radiusKm") double radiusKm,
            @Param("minLat") double minLat, @Param("maxLat") double maxLat,
            @Param("minLon") double minLon, @Param("maxLon") double maxLon,
            @Param("offset") int offset, @Param("limit") int limit
    );

    // 전체 개수 카운트 쿼리 - 네이티브 쿼리 사용
    @Query(value = """
    SELECT COUNT(*)
    FROM shelter s 
    WHERE s.latitude BETWEEN :minLat AND :maxLat 
      AND s.longitude BETWEEN :minLon AND :maxLon
      AND (6371 * acos(LEAST(1.0, GREATEST(-1.0, 
          cos(radians(:lat)) * cos(radians(s.latitude)) * 
          cos(radians(s.longitude) - radians(:lon)) + 
          sin(radians(:lat)) * sin(radians(s.latitude))
      )))) < :radiusKm
    """, nativeQuery = true)
    long countNearbySheltersPaged(
            @Param("lat") double lat, @Param("lon") double lon, @Param("radiusKm") double radiusKm,
            @Param("minLat") double minLat, @Param("maxLat") double maxLat,
            @Param("minLon") double minLon, @Param("maxLon") double maxLon
    );

}
