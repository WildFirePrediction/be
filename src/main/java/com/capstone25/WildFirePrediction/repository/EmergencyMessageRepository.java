package com.capstone25.WildFirePrediction.repository;

import com.capstone25.WildFirePrediction.domain.EmergencyMessage;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmergencyMessageRepository extends JpaRepository<EmergencyMessage, Long> {

    // 이미 저장된 재난문자인지 (시리얼 넘버 기준) 확인
    boolean existsBySerialNumber(Long serialNumber);

    // 가장 최근 시리얼 넘버 조회
    Optional<EmergencyMessage> findTopByOrderBySerialNumberDesc();

    // 특정날짜 기준으로 등록된 재난문자 개수 조회
    long countByRegDate(LocalDate regDate);
}
