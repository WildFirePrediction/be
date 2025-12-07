package com.capstone25.WildFirePrediction.domain;

import com.capstone25.WildFirePrediction.domain.base.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "ai_predicted_cell",
    indexes = {
        @Index(name = "idx_fire_id", columnList = "fire_id"),
        @Index(name = "idx_lat_lon", columnList = "latitude, longitude")
    })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class AIPredictedCell extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 예측된 셀의 위도
    @Column(nullable = false)
    private Double latitude;

    // 예측된 셀의 경도
    @Column(nullable = false)
    private Double longitude;

    // 타임스텝 정보 (1~5)
    @Column(nullable = false)
    private Integer timeStep;

    // 해당 타임스텝의 예상 시각
    @Column(nullable = false)
    private LocalDateTime predictedTimestamp;

    // 화재 확산 확률 (0.0 ~ 1.0)
    @Column(nullable = false)
    private Double probability;

    // 부모 화재 정보와의 N:1 관계
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fire_id", nullable = false, foreignKey = @ForeignKey(name = "fk_cell_fire"))
    private AIPredictionFire fire;


    // 양방향 관계 설정 메서드 (AIPredictionFire 쪽에서 호출)
    public void setFire(AIPredictionFire fire) {
        this.fire = fire;
    }
}
