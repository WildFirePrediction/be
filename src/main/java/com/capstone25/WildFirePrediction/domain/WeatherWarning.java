package com.capstone25.WildFirePrediction.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.io.Serializable;
import java.util.Objects;
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
@Table(
        name = "weather_warning",
        indexes = {
        // PK 직접 인덱스 활용 + 최적화
            @Index(name = "idx_branch_time", columnList = "brnch,prsntn_tm"),
            @Index(name = "idx_title", columnList = "title")
        },
        // 복합키 고유제약 자동 생성 + 추가 제약
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_weather_pk",
                        columnNames = {"brnch", "prsntn_tm", "prsntn_sn"}
                )
        }
)
public class WeatherWarning {

    @EmbeddedId
    private WeatherWarningId id;

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
    @Lob
    @Column(nullable = false)
    private String relevantZone;

    // 특보발효시각텍스트 (SPNE_FRMNT_TM_TXT)
    @Lob
    @Column(nullable = false)
    private String effectiveTimeText;

    // 발표내용 (PRSNTN_CN)
    @Lob
    private String content;

    // 특보발효현황시각 (SPNE_FRMNT_PRCON_TM)
    @Column(nullable = false)
    private String effectiveStatusTimeRaw;

    // 특보발효현황내용 (SPNE_FRMNT_PRCON_CN)
    @Lob
    @Column(nullable = false)
    private String effectiveStatusContent;

    // 예비특보발표현황 (RSRV_SPNE_PRSNTN_PRCON_)
    @Lob
    private String reservedWarningStatus;

    // 참고사항 (REF_MTTR)
    @Lob
    private String referenceMatter;

    // 행정안전부입수일시 (MAAS_OBNT_DT)
    @Column(nullable = false)
    private String maasObtainedAtRaw;

    @Embeddable
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class WeatherWarningId implements Serializable {

        // 지점 (BRNCH)
        @Column(name = "brnch", length = 22, nullable = false)
        private String branch;

        // 발표시각 (PRSNTN_TM, yyyyMMddHHmm)
        @Column(name = "prsntn_tm", length = 14, nullable = false)
        private String presentationTime;

        // 발표일련번호 (PRSNTN_SN)
        @Column(name = "prsntn_sn", length = 22, nullable = false)
        private String presentationSerial;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof WeatherWarningId that)) return false;
            return Objects.equals(branch, that.branch)
                    && Objects.equals(presentationTime, that.presentationTime)
                    && Objects.equals(presentationSerial, that.presentationSerial);
        }

        @Override
        public int hashCode() {
            return Objects.hash(branch, presentationTime, presentationSerial);
        }
    }
}
