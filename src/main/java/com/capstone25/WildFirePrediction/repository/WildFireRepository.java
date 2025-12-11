package com.capstone25.WildFirePrediction.repository;

import com.capstone25.WildFirePrediction.domain.WildFire;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface WildFireRepository extends JpaRepository<WildFire, Long> {
    boolean existsByFrstfrInfoId(String frstfrInfoId);

    @Query("select w.frstfrInfoId from WildFire w where w.frstfrInfoId in :ids")
    Set<String> findExistingIds(Set<String> ids);

    // 특정 날짜(발화일 기준) 데이터 조회용
    List<WildFire> findByIgnitionDateTimeBetween(LocalDateTime start, LocalDateTime end);
}
