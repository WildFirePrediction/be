package com.capstone25.WildFirePrediction.repository;

import com.capstone25.WildFirePrediction.domain.EmergencyMessage;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EmergencyMessageRepository extends JpaRepository<EmergencyMessage, Long> {

    // 일련번호로 단일 조회 (중복체크용)
    Optional<EmergencyMessage> findBySerialNumber(String serialNumber);

    // 일련번호 존재 여부 (저장 전 중복체크)
    boolean existsBySerialNumber(String serialNumber);

    // 최근 N개 조회 (페이지네이션)
    @Query(value = """
        SELECT * FROM emergency_message 
        ORDER BY created_at DESC 
        LIMIT :size OFFSET :offset
        """, nativeQuery = true)
    List<EmergencyMessage> findRecentMessages(
            @Param("offset") int offset,
            @Param("size") int size
    );

    // 지역별 최근 조회 (LIKE 검색)
    @Query(value = """
        SELECT * FROM emergency_message 
        WHERE region_name LIKE %:region%
        ORDER BY created_at DESC 
        LIMIT :size OFFSET :offset
        """, nativeQuery = true)
    List<EmergencyMessage> findRecentByRegion(
            @Param("region") String region,
            @Param("offset") int offset,
            @Param("size") int size
    );

    // 전체 개수 (페이징용)
    @Query(value = "SELECT COUNT(*) FROM emergency_message", nativeQuery = true)
    long countAll();

    // 지역별 개수
    @Query(value = "SELECT COUNT(*) FROM emergency_message WHERE region_name LIKE %:region%", nativeQuery = true)
    long countByRegion(@Param("region") String region);
}
