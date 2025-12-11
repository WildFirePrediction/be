package com.capstone25.WildFirePrediction.domain;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
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
        name = "region",
        indexes = {
                @Index(
                        name = "idx_region_sido_sigungu_eup",
                        columnList = "sido, sigungu, eupmyeondong"
                )
        }
)
public class Region {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 10)
    private String adminCode;

    @Column(nullable = false, length = 20)
    private String sido;

    @Column(nullable = false, length = 30)
    private String sigungu;

    @Column(nullable = false, length = 40)
    private String eupmyeondong;

    // 재난문자 ID
    @Column(name = "emergency_message_ids", columnDefinition = "JSON")
    private String emergencyMessageIdsJson = "[]";

    // 기상특보 ID
    @Column(name = "weather_warning_ids", columnDefinition = "JSON")
    private String weatherWarningIdsJson = "[]";

    private static final ObjectMapper objectMapper = new ObjectMapper();

    // 재난문자 JSON -> List<Long> 변환
    public List<Long> getEmergencyMessageIds() {
        try {
            if (emergencyMessageIdsJson == null || emergencyMessageIdsJson.equals("[]")) {
                return new ArrayList<>();
            }
            return objectMapper.readValue(emergencyMessageIdsJson, new TypeReference<List<Long>>() {});
        } catch (Exception e) {
            // JSON 파싱 실패 시 빈 리스트 반환
            return new ArrayList<>();
        }
    }

    // 재난문자 List에 ID 추가 -> JSON 저장
    public void addEmergencyMessageId(Long messageId) {
        try {
            List<Long> ids = getEmergencyMessageIds();
            if (!ids.contains(messageId)) {
                ids.add(messageId);
                emergencyMessageIdsJson = objectMapper.writeValueAsString(ids);
            }
        } catch (Exception e) {
            // 실패 시 빈 배열로 초기화
            emergencyMessageIdsJson = "[]";
        }
    }

    // 기상특보 JSON -> List<String> 변환
    public List<String> getWeatherWarningIds() {
        try {
            if (weatherWarningIdsJson == null || weatherWarningIdsJson.equals("[]")) {
                return new ArrayList<>();
            }
            return objectMapper.readValue(weatherWarningIdsJson, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    // 기상특보 List에 ID 추가 -> JSON 저장
    public void addWeatherWarningId(String warningId) {
        try {
            List<String> ids = getWeatherWarningIds();
            if (!ids.contains(warningId)) {
                ids.add(warningId);
                weatherWarningIdsJson = objectMapper.writeValueAsString(ids);
            }
        } catch (Exception e) {
            weatherWarningIdsJson = "[]";
        }
    }

    // 당일 기준 리셋 메서드
    public void resetDisasterIds() {
        emergencyMessageIdsJson = "[]";
        weatherWarningIdsJson = "[]";
    }
}
