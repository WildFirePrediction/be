package com.capstone25.WildFirePrediction.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
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
@Table(name = "emergency_message", indexes = {
        @Index(name = "idx_emergency_reg_date", columnList = "regDate"),
        @Index(name = "idx_emergency_serial", columnList = "serialNumber", unique = true)
})
public class EmergencyMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long serialNumber;

    @Column(nullable = false)
    private String messageContent;

    private String regionName;

    private LocalDateTime createdAt;  // CRT_DT 파싱

    private LocalDate regDate;        // REG_YMD 파싱

    private String stepName;          // EMRG_STEP_NM

    private String disasterTypeName;  // DST_SE_NM
}
