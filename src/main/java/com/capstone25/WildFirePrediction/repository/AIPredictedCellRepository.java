package com.capstone25.WildFirePrediction.repository;

import com.capstone25.WildFirePrediction.domain.AIPredictedCell;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface AIPredictedCellRepository {

    // 특정 화재의 모든 예측 셀 조회
    @Query("SELECT c FROM AIPredictedCell c WHERE c.fire.id = :fireId")
    List<AIPredictedCell> findByFireId(@Param("fireId") Long fireId);

    // 진행중인 모든 화재의 예측 셀 조회
    @Query("SELECT c FROM AIPredictedCell c " +
            "JOIN FETCH c.fire f " +
            "WHERE f.status = 'PROGRESS'")
    List<AIPredictedCell> findAllProgressFireCells();

    // 특정 좌표 범위 내의 예측 셀 조회 (바운딩 박스)
    @Query("SELECT c FROM AIPredictedCell c " +
            "JOIN FETCH c.fire f " +
            "WHERE f.status = 'PROGRESS' " +
            "AND c.latitude BETWEEN :minLat AND :maxLat " +
            "AND c.longitude BETWEEN :minLon AND :maxLon")
    List<AIPredictedCell> findCellsInBoundingBox(
            @Param("minLat") Double minLat,
            @Param("maxLat") Double maxLat,
            @Param("minLon") Double minLon,
            @Param("maxLon") Double maxLon
    );

    // 특정 좌표 범위 + 타임스텝 제한으로 예측 셀 조회
    @Query("SELECT c FROM AIPredictedCell c " +
            "JOIN FETCH c.fire f " +
            "WHERE f.status = 'PROGRESS' " +
            "AND c.timeStep <= :maxTimestep " +
            "AND c.latitude BETWEEN :minLat AND :maxLat " +
            "AND c.longitude BETWEEN :minLon AND :maxLon")
    List<AIPredictedCell> findCellsInBoundingBoxWithTimestep(
            @Param("minLat") Double minLat,
            @Param("maxLat") Double maxLat,
            @Param("minLon") Double minLon,
            @Param("maxLon") Double maxLon,
            @Param("maxTimestep") Integer maxTimestep
    );

    // 특정 화재의 예측 셀 개수 조회
    @Query("SELECT COUNT(c) FROM AIPredictedCell c WHERE c.fire.id = :fireId")
    long countByFireId(@Param("fireId") Long fireId);
}
