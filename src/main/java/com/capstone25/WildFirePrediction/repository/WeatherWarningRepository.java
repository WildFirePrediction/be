package com.capstone25.WildFirePrediction.repository;

import com.capstone25.WildFirePrediction.domain.WeatherWarning;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WeatherWarningRepository extends JpaRepository<WeatherWarning, WeatherWarning.WeatherWarningId> {

    // 3중 복합키 중복 체크
    @Query("SELECT w.id FROM WeatherWarning w WHERE w.id IN :ids")
    List<WeatherWarning.WeatherWarningId> findExistingIds(
            @Param("ids") List<WeatherWarning.WeatherWarningId> ids);

    // 날짜별 조회 (presentationTime 앞 8자리 = yyyyMMdd)
    @Query("SELECT w FROM WeatherWarning w " +
            "WHERE SUBSTRING(w.id.presentationTime, 1, 8) = :date")
    List<WeatherWarning> findByDate(@Param("date") String yyyymmdd);

    @Query("SELECT w FROM WeatherWarning w WHERE w.id IN :ids")
    List<WeatherWarning> findAllByIds(@Param("ids") List<WeatherWarning.WeatherWarningId> ids);
}
