package com.capstone25.WildFirePrediction.repository;

import com.capstone25.WildFirePrediction.domain.Region;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RegionRepository extends JpaRepository<Region, Long> {
    Optional<Region> findByAdminCode(String adminCode);

    // 시/도만으로 필터
    List<Region> findBySido(String sido);

    // 시/도 목록 DISTINCT
    @Query("SELECT DISTINCT r.sido FROM Region r ORDER BY r.sido")
    List<String> findDistinctSido();

    // 특정 시/도의 시군구 목록만 DISTINCT로 조회
    @Query("SELECT DISTINCT r.sigungu " +
            "FROM Region r " +
            "WHERE r.sido = :sido " +
            "ORDER BY r.sigungu")
    List<String> findDistinctSigunguBySido(@Param("sido") String sido);

    // 시/도 + 시/군/구
    List<Region> findBySidoAndSigungu(String sido, String sigungu);

    // 시/도 + 시/군/구 + 읍/면/동 (정확히 한 동 찾기)
    Optional<Region> findBySidoAndSigunguAndEupmyeondong(
            String sido, String sigungu, String eupmyeondong
    );
}
