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
        // 오늘 날짜 (YYYYMMDD)
        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        log.info("오늘({}) 재난문자 동기화 시작", today);

        try {
            // 1. 첫 페이지로 총 데이터 수 확인
            EmergencyMessageApiResponse firstPage = emergencyMessageApiService.fetchEmergencyMessagePage(1, today, null);
            int totalCount = firstPage.getTotalCount();
            int pageSize = firstPage.getNumOfRows();
            int totalPages = (int) Math.ceil((double) totalCount / pageSize);

            log.info("오늘 재난문자 총 {}건 ({}페이지)", totalCount, totalPages);
            if (totalCount == 0) {
                log.info("오늘 발송된 재난문자가 없습니다.");
                return;
            }

            // 2. 모든 페이지 순회
            int savedCount = 0;
            int skippedCount = 0;
            for(int pageNo = 1; pageNo <= totalPages; pageNo++) {
                EmergencyMessageApiResponse response = emergencyMessageApiService.fetchEmergencyMessagePage(pageNo, today, null);
                List<EmergencyMessageApiResponse.EmergencyMessageData> messageDataList = response.getBody();

                if (messageDataList == null || messageDataList.isEmpty()) {
                    log.warn("페이지 {} 비어있음", pageNo);
                    continue;
                }

                // 3. 현재 페이지 데이터 처리
                List<EmergencyMessage> messagesToSave = new ArrayList<>();
                for (EmergencyMessageApiResponse.EmergencyMessageData data : messageDataList) {
                    try {
                        EmergencyMessage message = convertToEntity(data);
                        if (emergencyMessageRepository.existsBySerialNumber(message.getSerialNumber())) {
                            skippedCount++;
                            continue;
                        }
                        messagesToSave.add(message);
                    } catch (Exception e) {
                        log.error("페이지 {} 데이터 변환 실패: {}", pageNo, data.getSerialNumber(), e);
                    }
                }

                // 4. 배치 저장
                if (!messagesToSave.isEmpty()) {
                    emergencyMessageRepository.saveAll(messagesToSave);
                    savedCount += messagesToSave.size();
                    log.info("페이지 {} 저장: {}건 (누적: {})", pageNo, messagesToSave.size(), savedCount);
                }
            }
            log.info("오늘 재난문자 동기화 완료 - 신규: {}건, 중복: {}건 (총 {}건)", savedCount, skippedCount, totalCount);
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
