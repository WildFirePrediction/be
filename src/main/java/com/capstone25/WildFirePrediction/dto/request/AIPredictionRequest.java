package com.capstone25.WildFirePrediction.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;


public class AIPredictionRequest {

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class FirePredictionRequestDto {
        // 이벤트 타입 : 0 (산불), 1 (종료)
        @NotBlank(message = "이벤트 타입은 필수입니다.")
        @Pattern(regexp = "^[01]$", message = "이벤트 타입은 0 또는 1이어야 합니다.")
        @JsonProperty("event_type")
        private String eventType;

        // 산림청 화재 고유 ID
        @NotBlank(message = "화재 ID는 필수입니다.")
        @JsonProperty("fire_id")
        private String fireId;

        // 화재 발생 위치
        @NotNull(message = "화재 발생 위치는 필수입니다.")
        @Valid
        @JsonProperty("fire_location")
        private FireLocationDto fireLocation;

        // 화재 발생 시각
        @NotNull(message = "화재 발생 시각은 필수입니다.")
        @JsonProperty("fire_timestamp")
        private LocalDateTime fireTimestamp;

        // ===== event_type="0" 전용 필드 =====
        // AI 추론 완료 시각
        @JsonProperty("inference_timestamp")
        private LocalDateTime inferenceTimestamp;

        // 사용된 AI 모델명
        @JsonProperty("model")
        private String model;

        // 타임스텝별 예측 결과 리스트
        @JsonProperty("predictions")
        @Valid
        private List<PredictionDto> predictions;


        // ===== event_type="1" 전용 필드 =====
        // 화재 종료 감지 시각
        @JsonProperty("ended_timestamp")
        private LocalDateTime endedTimestamp;

        // 산림처 공식 진화 완료 시각
        @JsonProperty("completion_timestamp")
        private LocalDateTime completionTimestamp;

        // 종료 이유
        @JsonProperty("end_reason")
        private String endReason;

        // 마지막 상태명 (저장X)
        @JsonProperty("last_status")
        private String lastStatus;

        // 마지막 상태 코드 (저장X)
        @JsonProperty("last_status_code")
        private String lastStatusCode;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class FireLocationDto {
        // 위도 (33 ~ 39)
        @NotNull(message = "위도는 필수입니다")
        @Min(value = 33, message = "위도는 33 이상이어야 합니다")
        @Max(value = 39, message = "위도는 39 이하이어야 합니다")
        @JsonProperty("lat")
        private Double lat;

        // 경도 (124 ~ 132)
        @NotNull(message = "경도는 필수입니다")
        @Min(value = 124, message = "경도는 124 이상이어야 합니다")
        @Max(value = 132, message = "경도는 132 이하이어야 합니다")
        @JsonProperty("lon")
        private Double lon;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PredictionDto {

        // 타임스텝 정보 (1~5)
        @NotNull(message = "타임스텝은 필수입니다")
        @Min(value = 1, message = "타임스텝은 1 이상이어야 합니다")
        @Max(value = 5, message = "타임스텝은 5 이하이어야 합니다")
        @JsonProperty("timestep")
        private Integer timestep;

        // 해당 타임스텝의 예상 시각
        @NotNull(message = "예상 시각은 필수입니다")
        @JsonProperty("timestamp")
        private LocalDateTime timestamp;

        // 예측된 셀 리스트
        @NotEmpty(message = "예측 셀은 최소 1개 이상이어야 합니다")
        @Valid
        @JsonProperty("predicted_cells")
        private List<PredictedCellDto> predictedCells;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PredictedCellDto {
        // 예측된 셀의 위도
        @NotNull(message = "예측된 셀의 위도는 필수입니다")
        @Min(value = 33, message = "예측된 셀의 위도는 33 이상이어야 합니다")
        @Max(value = 39, message = "예측된 셀의 위도는 39 이하이어야 합니다")
        @JsonProperty("lat")
        private Double lat;

        // 예측된 셀의 경도
        @NotNull(message = "예측된 셀의 경도는 필수입니다")
        @Min(value = 124, message = "예측된 셀의 경도는 124 이상이어야 합니다")
        @Max(value = 132, message = "예측된 셀의 경도는 132 이하이어야 합니다")
        @JsonProperty("lon")
        private Double lon;

        // 화재 확산 확률 (0.0 ~ 1.0)
        @NotNull(message = "화재 확산 확률은 필수입니다")
        @JsonProperty("probability")
        private Double probability;
    }
}
