package com.capstone25.WildFirePrediction.repository;

import com.capstone25.WildFirePrediction.domain.WeatherWarning;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WeatherWarningRepository extends JpaRepository<WeatherWarning, Long> {

    // 발표시각 + 일련번호로 단일 조회 (중복 체크용)
    Optional<WeatherWarning> findByPresentationTimeAndPresentationSerial(
            LocalDateTime presentationTime,
            Long presentationSerial
    );

    // 발표시각 + 일련번호 존재 여부 (저장 전 중복 체크)
    boolean existsByPresentationTimeAndPresentationSerial(
            LocalDateTime presentationTime,
            Long presentationSerial
    );

    // 발표시각 + 일련번호 키 리스트로 존재하는 키 조회 (배치 중복 체크용)
    @Query("SELECT CONCAT(w.presentationTime, '_', w.presentationSerial) " +
            "FROM WeatherWarning w WHERE CONCAT(w.presentationTime, '_', w.presentationSerial) IN :keys")
    List<String> findExistingKeys(@Param("keys") List<String> keys);

    // 최근 N개 조회 (발표시각 내림차순, 페이지네이션)
    @Query(value = """
        SELECT * FROM weather_warning
        ORDER BY presentation_time DESC
        LIMIT :size OFFSET :offset
        """, nativeQuery = true)
    List<WeatherWarning> findRecentWarnings(
            @Param("offset") int offset,
            @Param("size") int size
    );

    // 지점별 최근 조회
    @Query(value = """
        SELECT * FROM weather_warning
        WHERE branch = :branch
        ORDER BY presentation_time DESC
        LIMIT :size OFFSET :offset
        """, nativeQuery = true)
    List<WeatherWarning> findRecentByBranch(
            @Param("branch") int branch,
            @Param("offset") int offset,
            @Param("size") int size
    );

    // 전체 개수
    @Query(value = "SELECT COUNT(*) FROM weather_warning", nativeQuery = true)
    long countAll();

    // 지점별 개수
    @Query(value = "SELECT COUNT(*) FROM weather_warning WHERE branch = :branch", nativeQuery = true)
    long countByBranch(@Param("branch") int branch);

}
