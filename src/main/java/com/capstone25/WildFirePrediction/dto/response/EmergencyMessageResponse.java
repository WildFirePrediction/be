package com.capstone25.WildFirePrediction.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmergencyMessageResponse {

    private String serialNumber;     // 일련번호
    private String messageContent;   // 메시지 내용
    private String regionName;       // 지역명
    private String stepName;         // 긴급단계명
    private String disasterTypeName; // 재해구분명
    private LocalDateTime createdAt; // 생성일시 (파싱된 날짜)
}