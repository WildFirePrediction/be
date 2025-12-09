package com.capstone25.WildFirePrediction.repository;

import com.capstone25.WildFirePrediction.domain.Shelter;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ShelterRepository extends JpaRepository<Shelter, Long> {

    // 관리일련번호로 대피소 조회
    Optional<Shelter> findByManagementNumber(String managementNumber);

    // 관리일련번호 존재 여부 확인 (중복 방지)
    boolean existsByManagementNumber(String managementNumber);
}
