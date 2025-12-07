package com.capstone25.WildFirePrediction.repository;

import com.capstone25.WildFirePrediction.domain.AIPredictionFire;
import com.capstone25.WildFirePrediction.domain.enums.FireStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface AIPredictionFireRepository extends JpaRepository<AIPredictionFire, Long> {

    // 산림청 화재 ID로 화재 정보 조회 (중복 요청 체크 시 사용)
    Optional<AIPredictionFire> findByFireId(String fireId);

    // 산림청 화재 ID로 화재 존재 여부 확인 (중복 체크 시 사용)
    boolean existsByFireId(String fireId);

    // 특정 상태의 모든 화재 조회
    List<AIPredictionFire> findByStatus(FireStatus status);

    // 진행중인 모든 화재 조회 (예측셀 포함)
    @Query("SELECT DISTINCT f FROM AIPredictionFire f"
            + " LEFT JOIN FETCH f.predictedCells"
            + " WHERE f.status = 'PROGRESS'")
    List<AIPredictionFire> findAllProgressFiresWithCells();

    // 특정 화재 ID와 상태로 조회
    Optional<AIPredictionFire> findByFireIdAndStatus(String fireId, FireStatus status);

    // 진행중인 화재 개수 조회
    @Query("SELECT COUNT(f) FROM AIPredictionFire f WHERE f.status = 'PROGRESS'")
    long countProgressFiresCount();
}
