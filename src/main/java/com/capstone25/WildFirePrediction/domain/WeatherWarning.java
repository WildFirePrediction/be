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
@Table(name = "weather_warning", indexes = {
        @Index(name = "idx_weather_prsntn_time", columnList = "presentationTime"),
        @Index(name = "idx_weather_branch", columnList = "branch"),
        @Index(name = "idx_weather_unique", columnList = "presentationTime, presentationSerial", unique = true)
})
public class WeatherWarning {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 지점 (BRNCH)
    @Column(nullable = false)
    private Integer branch;

    // 발표시각 (PRSNTN_TM, yyyyMMddHHmm → LocalDateTime으로 파싱)
    @Column(nullable = false)
    private LocalDateTime presentationTime;

    // 발표일련번호 (PRSNTN_SN) - 통보문 고유 식별용
    @Column(nullable = false)
    private Long presentationSerial;

    // 예보관 (FCAST)
    @Column(nullable = false)
    private String forecasterName;

    // 특보발표코드 (SPNE_PRSNTN_CD)
    @Column(nullable = false, length = 2)
    private String warningPresentationCode;

    // 제목 (TTL)
    @Column(nullable = false)
    private String title;

    // 해당구역 (RLVT_ZONE)
    @Column(nullable = false, columnDefinition = "TEXT")
    private String relevantZone;

    // 특보발효시각텍스트 (SPNE_FRMNT_TM_TXT)
    @Column(nullable = false, columnDefinition = "TEXT")
    private String effectiveTimeText;

    // 발표내용 (PRSNTN_CN)
    @Column(columnDefinition = "TEXT")
    private String content;

    // 특보발효현황시각 (SPNE_FRMNT_PRCON_TM) - 문자열 포맷 복잡 → 우선 String으로 저장
    @Column(nullable = false)
    private String effectiveStatusTimeRaw;

    // 특보발효현황내용 (SPNE_FRMNT_PRCON_CN)
    @Column(nullable = false, columnDefinition = "TEXT")
    private String effectiveStatusContent;

    // 예비특보발표현황 (RSRV_SPNE_PRSNTN_PRCON_)
    @Column(columnDefinition = "TEXT")
    private String reservedWarningStatus;

    // 참고사항 (REF_MTTR)
    @Column(columnDefinition = "TEXT")
    private String referenceMatter;

    // 행정안전부입수일시 (MAAS_OBNT_DT) - 우선 원본 문자열 그대로 저장
    @Column(nullable = false)
    private String maasObtainedAtRaw;
}
