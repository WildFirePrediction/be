package com.capstone25.WildFirePrediction.repository;

import com.capstone25.WildFirePrediction.domain.WeatherWarning;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WeatherWarningRepository extends JpaRepository<WeatherWarning, Long> {

    // 중복된 특보 데이터가 있는지 확인
    boolean existsByTitleAndBaseDateAndPresentationTime(
            String title,
            LocalDate baseDate,
            LocalDateTime presentationTime
    );

    // 특정 기준일에 해당하는 특보 데이터 개수 조회
    long countByBaseDate(LocalDate baseDate);
}
