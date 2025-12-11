package com.capstone25.WildFirePrediction.service;

import com.capstone25.WildFirePrediction.domain.EmergencyMessage;
import com.capstone25.WildFirePrediction.dto.response.EmergencyMessageApiResponse;
import com.capstone25.WildFirePrediction.global.code.status.ErrorStatus;
import com.capstone25.WildFirePrediction.global.exception.handler.ExceptionHandler;
import com.capstone25.WildFirePrediction.repository.EmergencyMessageRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmergencyMessageService {
    private final EmergencyMessageApiService emergencyMessageApiService;
    private final EmergencyMessageRepository emergencyMessageRepository;

    private static final DateTimeFormatter CRT_DT_FORMATTER = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
    private static final DateTimeFormatter REG_YMD_FORMATTER = DateTimeFormatter.ofPattern("yyyy/MM/dd");

    // 원시 재난문자 데이터 조회 (테스트용 - JSON 그대로 반환)
    public String loadRawEmergencyMessages(String dateStr) {
        log.info("원시 재난문자 조회 시작 - crtDt: {}", dateStr);

        try {
            EmergencyMessageApiResponse response =
                    emergencyMessageApiService.fetchEmergencyMessagePage(1, dateStr, null);

            int totalCount = response.getTotalCount();
            int pageSize = response.getNumOfRows();

            log.info("API 응답 - totalCount: {}, numOfRows: {}, 실제 데이터: {}건",
                    totalCount, pageSize,
                    response.getBody() != null ? response.getBody().size() : 0);

            // JSON으로 변환해서 리턴
            ObjectMapper mapper = new ObjectMapper();
            mapper.enable(SerializationFeature.INDENT_OUTPUT); // 보기 좋게

            return mapper.writeValueAsString(response);

        } catch (Exception e) {
            log.error("원시 재난문자 조회 실패 - crtDt: {}", dateStr, e);
            return "{ \"error\": \"" + e.getMessage() + "\" }";
        }
    }

    // (임시) 날짜 입력 받아서 해당 날짜 재난문자 데이터 수집
    public void loadTodaysEmergencyMessages() {
        // 오늘 날짜 (YYYYMMDD)
        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        loadEmergencyMessagesByDate(today);
    }

    @Transactional
    public void loadEmergencyMessagesByDate(String dateStr) {
        try {
            // 1. 첫 페이지로 총 데이터 수 확인
            EmergencyMessageApiResponse firstPage = emergencyMessageApiService.fetchEmergencyMessagePage(1, dateStr, null);
            int totalCount = firstPage.getTotalCount();
            int pageSize = firstPage.getNumOfRows();
            int totalPages = (int) Math.ceil((double) totalCount / pageSize);

            log.info("재난문자 총 {}건 ({}페이지)", totalCount, totalPages);
            if (totalCount == 0) {
                log.info("발송된 재난문자가 없습니다.");
                return;
            }

            // 2. DB 기존 serialNumber들 조회 (N+1 해결 핵심!)
            List<String> firstPageSerialNumbers = firstPage.getBody().stream()
                    .map(EmergencyMessageApiResponse.EmergencyMessageData::getSerialNumber)
                    .filter(Objects::nonNull)
                    .toList();
            Set<String> existingSerialNumbers = new HashSet<>(
                    emergencyMessageRepository.findExistingSerialNumbers(firstPageSerialNumbers)
            ); // 첫 페이지 serialNumber로 DB 조회

            log.info("DB 기존 데이터: {}건 / API 첫페이지: {}건", existingSerialNumbers.size(), firstPageSerialNumbers.size());

            // 3. 페이지별 처리 (동일)
            int savedCount = 0;
            int skippedCount = 0;
            for (int pageNo = 1; pageNo <= totalPages; pageNo++) {
                int[] skippedHolder = new int[1];

                List<EmergencyMessage> messagesToSave = processPage(pageNo, dateStr, existingSerialNumbers, skippedHolder);
                if (!messagesToSave.isEmpty()) {
                    emergencyMessageRepository.saveAll(messagesToSave);
                    savedCount += messagesToSave.size();
                    // 저장된 serialNumber를 Set에 추가 (중복 방지)
                    messagesToSave.forEach(msg -> existingSerialNumbers.add(msg.getSerialNumber()));
                }
                skippedCount += skippedHolder[0];
                log.info("페이지 {} 저장: {}건 (누적: {})", pageNo, messagesToSave.size(), savedCount);
            }

            log.info("재난문자 동기화 완료 - crtDt={}, 신규: {}건, 중복: {}건", dateStr, savedCount, skippedCount);
        } catch (Exception e) {
            log.error("재난문자 동기화 실패", e);
            throw new ExceptionHandler(ErrorStatus.EMERGENCY_DATA_LOAD_FAILED);
        }
    }

    // 페이지별 처리 (메모리 Set으로 O(1) 비교)
    private List<EmergencyMessage> processPage(int pageNo, String today, Set<String> existingSerialNumbers, int[] skippedCountHolder) {
        EmergencyMessageApiResponse response = emergencyMessageApiService.fetchEmergencyMessagePage(pageNo, today, null);
        List<EmergencyMessage> messagesToSave = new ArrayList<>();

        int skippedThisPage = 0;

        for (EmergencyMessageApiResponse.EmergencyMessageData data : response.getBody()) {
            String serialNumber = data.getSerialNumber();
            if (serialNumber == null || existingSerialNumbers.contains(serialNumber)) {
                skippedThisPage++;
                continue;
            }
            try {
                messagesToSave.add(convertToEntity(data));
            } catch (Exception e) {
                log.error("페이지 {} 변환 실패: {}", pageNo, serialNumber, e);
            }
        }

        skippedCountHolder[0] += skippedThisPage; // 누적

        return messagesToSave;
    }

    private EmergencyMessage convertToEntity(EmergencyMessageApiResponse.EmergencyMessageData data) {
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
    }

    // CRT_DT 파싱 (YYYY/MM/DD HH:mm:ss)
    private LocalDateTime parseDateTime(String dateTimeStr) {
        if (dateTimeStr == null || dateTimeStr.trim().isEmpty()) {
            return null;
        }
        try {
            return LocalDateTime.parse(dateTimeStr.trim(), CRT_DT_FORMATTER);
        } catch (DateTimeParseException e) {
            log.warn("날짜 파싱 실패 (CRT_DT): '{}'", dateTimeStr);
            return null;
        }
    }

    // REG_YMD 파싱 (예: "2025/12/10 15:32:09.000000000" → "2025/12/10")
    private LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            return null;
        }
        try {
            String trimmed = dateStr.trim();
            String datePart = trimmed.split(" ")[0];  // "2025/12/10"
            return LocalDate.parse(datePart, REG_YMD_FORMATTER);
        } catch (DateTimeParseException e) {
            log.warn("날짜 파싱 실패 (REG_YMD): '{}'", dateStr);
            return null;
        }
    }

    // 10분마다 최신 재난문자 데이터 수집
    @Scheduled(cron = "0 0/10 * * * *")
    public void scheduledFetchTodayMessages() {
        log.info("[재난문자] 10분 주기 수집 시작");
        try {
            loadTodaysEmergencyMessages();
            log.info("[재난문자] 10분 주기 수집 완료");
        } catch (Exception e) {
            log.error("[재난문자] 스케줄링 수집 중 예외 발생", e);
            // 다음 실행까지 계속 동작 보장
        }
    }
}
