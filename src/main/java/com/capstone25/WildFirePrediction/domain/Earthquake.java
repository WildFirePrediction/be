package com.capstone25.WildFirePrediction.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Earthquake {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 공공API의 지진번호(또는 생성일련번호)로 중복 체크
    @Column(unique = true, nullable = false)
    private String earthquakeNo;     // ERQK_NO

    private String branchNo;         // BRNCH_NO
    private String disasterTypeKind; // MSTN_TYPE_KND

    private LocalDateTime occurrenceTime; // PRSNTN_TM 또는 INPT_TM 등을 변환해서 저장
    private Double latitude;        // LAT
    private Double longitude;       // LOT
    private String position;        // PSTN

    private Double scale;           // SCL
    private Double depthKm;         // OCRN_DPTH

    private String notificationLevel; // NTFCTN_LVL
    private String refNo;            // REF_NO
    private String refMatter;        // REF_MTTR
    private String modificationMatter; // MDFCN_MTTR

    private LocalDateTime maasObtainedAt; // MAAS_OBNT_DT
}
