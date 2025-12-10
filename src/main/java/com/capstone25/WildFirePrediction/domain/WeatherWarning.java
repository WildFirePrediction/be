package com.capstone25.WildFirePrediction.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(name = "weather_warning",
        indexes = {
            @Index(name = "idx_weather_base_date", columnList = "baseDate"),
            @Index(name = "idx_weather_title", columnList = "title")
        },
        uniqueConstraints = {
            @UniqueConstraint(
                name = "uk_weather_warning_unique",
                columnNames = {"title", "baseDate", "presentationTime"}
            )
        }
)
public class WeatherWarning {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 예: "대설주의보 해제", "풍랑주의보 발표" 등
    @Column(nullable = false)
    private String title;

    private String relevantZone;   // 대상 지역 설명

    private LocalDate baseDate;    // SPNE_FRMNT_PRCON_TM (특보 기준일)

    private LocalDateTime presentationTime; // PRSNTN_TM 파싱

    private String currentAlertContent; // SPNE_FRMNT_PRCON_CN

    private String reservedAlertContent; // RS(S)RV_SPNE_PRSNTN_PRCON_

    private String presentationCode; // SPNE_PRSNTN_CD

    private LocalDate obtainedDate;  // MAAS_OBNT_DT

    private String referenceMatter; // REF_MTTR

    private String presentationContent; // PRSNTN_CN

    private String forecaster; // FCAST
}
