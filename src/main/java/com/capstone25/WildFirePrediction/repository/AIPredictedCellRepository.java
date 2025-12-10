package com.capstone25.WildFirePrediction.repository;

import com.capstone25.WildFirePrediction.domain.AIPredictedCell;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface AIPredictedCellRepository extends JpaRepository<AIPredictedCell, Long> {

    // 특정 화재의 모든 예측 셀 조회
    @Query("SELECT c FROM AIPredictedCell c WHERE c.fire.id = :fireId")
    List<AIPredictedCell> findByFireId(@Param("fireId") Long fireId);

    // 특정 화재의 모든 예측 셀 삭제
    @Modifying
    @Query("DELETE FROM AIPredictedCell c WHERE c.fire.id = :fireId")
    int deleteAllByFireId(@Param("fireId") Long fireId);

    // 여러 화재의 예측 셀 일괄 삭제
    @Modifying
    @Query("DELETE FROM AIPredictedCell c WHERE c.fire.id IN :fireIds")
    int deleteAllByFireIds(@Param("fireIds") List<Long> fireIds);

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

    // 공간 인덱스 쿼리
    @Query(value = """
        SELECT c.* FROM ai_predicted_cell c
        JOIN ai_prediction_fire f ON c.fire_id = f.id
        WHERE MBRContains(
            ST_GeomFromText(:bboxPolygon, 0),
            c.geom
        ) 
        AND f.status = 'PROGRESS'
        AND STR_TO_DATE(c.predicted_timestamp, '%Y-%m-%dT%H:%i:%s') 
            BETWEEN CURRENT_TIMESTAMP 
            AND DATE_ADD(CURRENT_TIMESTAMP, INTERVAL 1 HOUR)
        AND c.probability > :minProbability
        ORDER BY ST_Distance_Sphere(c.geom, ST_GeomFromText(:centerPoint, 0))
        LIMIT 100
        """, nativeQuery = true)
    List<AIPredictedCell> findDangerCellsNearRoute(
            @Param("bboxPolygon") String bboxPolygon,
            @Param("centerPoint") String centerPoint,
            @Param("minProbability") double minProbability
    );
}
