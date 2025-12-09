package com.capstone25.WildFirePrediction.repository;

import com.capstone25.WildFirePrediction.domain.Shelter;
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

    // 위치 기준 반경 내 대피소 검색 (Haversine 공식 활용)
    @Query(value = """
    SELECT s.id, s.facility_name, s.road_address, s.latitude, s.longitude, 
           s.shelter_type_name,
           (6371 * acos(cos(radians(:lat)) * 
           cos(radians(s.latitude)) * 
           cos(radians(s.longitude) - radians(:lon)) + 
           sin(radians(:lat)) * 
           sin(radians(s.latitude)))) AS distance_km
    FROM shelter s 
    HAVING distance_km < :radiusKm
    ORDER BY distance_km ASC
    LIMIT 100
    """, nativeQuery = true)
    List<Object[]> findNearbyShelters(
            @Param("lat") double lat,
            @Param("lon") double lon,
            @Param("radiusKm") double radiusKm
    );

    // 반경 내 대피소 개수 확인
    @Query(value = """
    SELECT COUNT(*)
    FROM shelter s 
    WHERE (6371 * acos(cos(radians(:lat)) * 
    cos(radians(s.latitude)) * 
    cos(radians(s.longitude) - radians(:lon)) + 
    sin(radians(:lat)) * 
    sin(radians(s.latitude)))) < :radiusKm
    """, nativeQuery = true)
    long countNearbyShelters(
            @Param("lat") double lat,
            @Param("lon") double lon,
            @Param("radiusKm") double radiusKm
    );
}
