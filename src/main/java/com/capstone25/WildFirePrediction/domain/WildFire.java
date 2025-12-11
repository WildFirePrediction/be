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
public class WildFire {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 공공API의 FRSTFR_INFO_ID (중복 체크용)
    @Column(unique = true, nullable = false)
    private String frstfrInfoId;

    private LocalDateTime ignitionDateTime;    // FRSTFR_GNT_DT
    private String address;                    // FRSTFR_DCLR_ADDR
    private Double x;                          // FRSTFR_PSTN_XCRD
    private Double y;                          // FRSTFR_PSTN_YCRD

    private String sidoCode;                   // STDG_CTPV_CD
    private String sigunguCode;                // STDG_SGG_CD

    private Double damageArea;                 // GRS_FRSTFR_DAM_AREA
    private Long damageAmount;                 // FRSTFR_GRS_DAM_AMT

    private LocalDateTime maasObtainedAt;      // MAAS_OBTN_YMD
}
