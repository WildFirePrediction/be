package com.capstone25.WildFirePrediction.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "shelter", indexes = {
        @Index(name = "idx_location", columnList = "latitude, longitude"),
        @Index(name = "idx_shelter_type", columnList = "shelter_type_code")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Shelter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String managementNumber;    // 관리일련번호 (MNG_SN) - 중복 방지용

    @Column(nullable = false)
    private String facilityName;    // 시설명 (REARE_NM)

    @Column(nullable = false)
    private String roadAddress;     // 도로명전체주소 (RONA_DADDR)

    @Column(nullable = false)
    private Double latitude;        // 위도

    @Column(nullable = false)
    private Double longitude;       // 경도

    @Column(nullable = false, length = 1)
    private String shelterTypeCode;  // 대피소구분코드 (SHLT_SE_CD)

    @Column(nullable = false)
    private String shelterTypeName;  // 대피소구분명 (SHLT_SE_NM)
}
