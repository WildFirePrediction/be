package com.capstone25.WildFirePrediction.repository;

import com.capstone25.WildFirePrediction.domain.WeatherWarning;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WeatherWarningRepository extends JpaRepository<WeatherWarning, WeatherWarning.WeatherWarningId> {

    // 3중 복합키 중복 체크
    @Query(value = """
        SELECT CONCAT(w.brnch, '_', w.prsntn_tm, '_', w.prsntn_sn) 
        FROM weather_warning w 
        WHERE CONCAT(w.brnch, '_', w.prsntn_tm, '_', w.prsntn_sn) IN :keys
        """, nativeQuery = true)
    List<String> findExistingKeys(@Param("keys") List<String> keys);
}
