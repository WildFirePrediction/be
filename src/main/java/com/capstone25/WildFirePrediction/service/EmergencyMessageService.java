package com.capstone25.WildFirePrediction.service;

import com.capstone25.WildFirePrediction.domain.EmergencyMessage;
import com.capstone25.WildFirePrediction.dto.response.EmergencyMessageApiResponse;
import com.capstone25.WildFirePrediction.global.code.status.ErrorStatus;
import com.capstone25.WildFirePrediction.global.exception.handler.ExceptionHandler;
import com.capstone25.WildFirePrediction.repository.EmergencyMessageRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class EmergencyMessageService {
    private final EmergencyMessageApiService emergencyMessageApiService;
    private final EmergencyMessageRepository emergencyMessageRepository;

    public void loadTodaysEmergencyMessages() {
        // 1. 오늘 날짜 (YYYYMMDD)
        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        log.info("오늘({}) 재난문자 동기화 시작", today);

        try {
            // 2. 오늘 데이터 전체 가져오기
            EmergencyMessageApiResponse response = emergencyMessageApiService.fetchEmergencyMessagePage(1, today, null);
            int totalCount = response.getTotalCount();

            log.info("오늘 재난문자 총 {}건 조회", totalCount);

            if (totalCount == 0) {
                log.info("오늘 발송된 재난문자가 없습니다.");
                return;
            }

            // 3. DB에 저장
            List<EmergencyMessageApiResponse.EmergencyMessageData> messageDataList = response.getBody();
            int savedCount = 0;
            int skippedCount = 0;

            List<EmergencyMessage> messagesToSave = new ArrayList<>();
            for (EmergencyMessageApiResponse.EmergencyMessageData data : messageDataList) {
                try {
                    EmergencyMessage message = convertToEntity(data);

                    // 중복 체크 (SerialNumber 기준)
                    if (emergencyMessageRepository.existsBySerialNumber(message.getSerialNumber())) {
                        skippedCount++;
                        log.debug("중복 스킵: {}", message.getSerialNumber());
                        continue;
                    }

                    messagesToSave.add(message);
                } catch (Exception e) {
                    log.error("데이터 변환 실패: {}", data.getSerialNumber(), e);
                }
            }

            // 4. 신규 데이터만 저장
            if (!messagesToSave.isEmpty()) {
                emergencyMessageRepository.saveAll(messagesToSave);
                savedCount = messagesToSave.size();
                log.info("오늘 재난문자 저장 완료: {}건 신규 / {}건 중복", savedCount, skippedCount);
            } else {
                log.info("오늘 재난문자 모두 중복되어 저장없음 (총 {}건 중복)", skippedCount);
            }
        } catch (Exception e) {
            log.error("오늘 재난문자 동기화 실패", e);
            throw new ExceptionHandler(ErrorStatus.EMERGENCY_DATA_LOAD_FAILED);
        }
    }

    private EmergencyMessage convertToEntity(EmergencyMessageApiResponse.EmergencyMessageData data) {
        try {
            LocalDateTime createdAt = parseDateTime(data.getCreatedAtStr());
            LocalDate regDate = parseDate(data.getRegDateStr());

            return EmergencyMessage.builder()
                    .serialNumber(data.getSerialNumber())
                    .messageContent(data.getMessageContent())
                    .regionName(data.getRegionName())
                    .stepName(data.getStepName())
                    .disasterTypeName(data.getDisasterTypeName())
                    .createdAt(createdAt)
                    .regDate(regDate)
                    .build();
        } catch (IllegalArgumentException e) {
            throw new ExceptionHandler(ErrorStatus.EMERGENCY_API_RESPONSE_INVALID);
        }
    }

    // CRT_DT 파싱 (yyyyMMddHHmmss 또는 yyyy-MM-dd HH:mm:ss)
    private LocalDateTime parseDateTime(String dateTimeStr) {
        if (dateTimeStr == null || dateTimeStr.trim().isEmpty()) {
            return null;
        }

        try {
            // 패턴1: 20231201123045
            DateTimeFormatter formatter1 = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
            if (dateTimeStr.length() == 14) {
                return LocalDateTime.parse(dateTimeStr, formatter1);
            }

            // 패턴2: 2023-12-01 12:30:45
            DateTimeFormatter formatter2 = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            return LocalDateTime.parse(dateTimeStr.trim(), formatter2);

        } catch (DateTimeParseException e) {
            log.warn("날짜 파싱 실패 (CRT_DT): '{}'", dateTimeStr);
            return null;
        }
    }

    // REG_YMD 파싱 (YYYYMMDD)
    private LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            return null;
        }

        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
            return LocalDate.parse(dateStr.trim(), formatter);
        } catch (DateTimeParseException e) {
            log.warn("날짜 파싱 실패 (REG_YMD): '{}'", dateStr);
            return null;
        }
    }

    // 5분마다 최신 재난문자 데이터 수집
    @Scheduled(cron = "0 0/5 * * * *")  // 매 5분 0초
    public void scheduledFetchTodayMessages() {
        log.info("[재난문자] 5분 주기 수집 시작");
        try {
            loadTodaysEmergencyMessages();
            log.info("[재난문자] 5분 주기 수집 완료");
        } catch (Exception e) {
            log.error("[재난문자] 스케줄링 수집 중 예외 발생", e);
            // 다음 실행까지 계속 동작 보장
        }
    }
}
