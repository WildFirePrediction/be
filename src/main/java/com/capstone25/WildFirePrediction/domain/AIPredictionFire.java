package com.capstone25.WildFirePrediction.domain;

import com.capstone25.WildFirePrediction.domain.base.BaseEntity;
import com.capstone25.WildFirePrediction.domain.enums.FireStatus;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "ai_prediction_fire",
    indexes = {
        @Index(name = "idx_fire_id", columnList = "fire_id"),
        @Index(name = "idx_status", columnList = "status")
    })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class AIPredictionFire extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 산림청 화재 고유 ID
    @Column(nullable = false, unique = true)
    private String fireId;

    // 이벤트 타입 : 0 (산불), 1 (종료)
    @Column(nullable = false, length = 1)
    private String eventType;

    // 화재 발생 위치 (위도)
    @Column(nullable = false)
    private Double fireLatitude;

    // 화재 발생 위치 (경도)
    @Column(nullable = false)
    private Double fireLongitude;

    // 화재 발생 시간 (산림청 제공)
    @Column(nullable = false)
    private LocalDateTime fireTimestamp;

    // AI 추론 완료 시각 (event_type = 0)
    private LocalDateTime inferenceTimestamp;

    // 사용된 AI 모델명 (event_type = 0)
    private String model;

    // 화재 상태 (PROGRESS, END)
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FireStatus status;

    // 종료 이유 (event_type = 1)
    private String endReason;

    // 화재 종료 감지 시각 (event_type = 1)
    private LocalDateTime endedTimestamp;

    // 산림청 공식 진화 완료 시각 (event_type = 1)
    private LocalDateTime completionTimestamp;

    // 예측 셀 리스트 (1:N)
    @OneToMany(mappedBy = "fire", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<AIPredictedCell> predictedCells = new ArrayList<>();


    // 화재 예측 데이터 업데이트 메소드
    public void updatePredictionData(
            String eventType,
            Double fireLatitude,
            Double fireLongitude,
            LocalDateTime fireTimestamp,
            LocalDateTime inferenceTimestamp,
            String model) {

        this.eventType = eventType;
        this.fireLatitude = fireLatitude;
        this.fireLongitude = fireLongitude;
        this.fireTimestamp = fireTimestamp;
        this.inferenceTimestamp = inferenceTimestamp;
        this.model = model;
    }

    // 재발화
    public void reactivateFire() {
        this.status = FireStatus.PROGRESS;
        this.endReason = null;
        this.endedTimestamp = null;
        this.completionTimestamp = null;
    }

    // 화재 상태 'END'로 변경
    public void endFire(String endReason, LocalDateTime endedTimestamp, LocalDateTime completionTimestamp) {
        this.status = FireStatus.END;
        this.endReason = endReason;
        this.endedTimestamp = endedTimestamp;
        this.completionTimestamp = completionTimestamp;
    }

    // 예측 셀 추가 메소드
    public void addPredictedCell(AIPredictedCell cell) {
        predictedCells.add(cell);
        cell.setFire(this);
    }
}
