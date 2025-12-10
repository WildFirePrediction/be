package com.capstone25.WildFirePrediction.service;

import com.capstone25.WildFirePrediction.domain.EmergencyMessage;
import com.capstone25.WildFirePrediction.dto.EmergencyMessageDto;
import com.capstone25.WildFirePrediction.dto.response.PublicApiResponse;
import com.capstone25.WildFirePrediction.global.code.status.ErrorStatus;
import com.capstone25.WildFirePrediction.global.exception.handler.ExceptionHandler;
import com.capstone25.WildFirePrediction.repository.EmergencyMessageRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
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

    @Transactional
    public void fetchAndSaveTodayMessages() {
        LocalDate today = LocalDate.now();
        log.info("[재난문자] 오늘 데이터 수집 시작 - {}", today);

        // 1. 1페이지만 받아오고, 필요하면 나중에 2페이지 확장
        PublicApiResponse.PagedResponse<EmergencyMessageDto> page =
                emergencyMessageApiService.fetchPage(1);

        if (page == null || page.getBody() == null || page.getBody().isEmpty()) {
            log.info("[재난문자] 오늘 API 응답 비어 있음");
            return;
        }

        List<EmergencyMessageDto> todayMessages = page.getBody().stream()
                .filter(dto -> today.toString().equals(dto.getRegDate()))
                .toList();

        if (todayMessages.isEmpty()) {
            log.info("[재난문자] 오늘 REG_YMD={} 인 데이터 없음", today);
            return;
        }

        int saved = 0;
        int skipped = 0;

        for (EmergencyMessageDto dto : todayMessages) {
            try {
                if (dto.getSerialNumber() == null) {
                    log.warn("[재난문자] SN 누락 데이터 스킵: {}", dto.getMessageContent());
                    skipped++;
                    continue;
                }

                // 이미 저장된 SN이면 스킵
                if (emergencyMessageRepository.existsBySerialNumber(dto.getSerialNumber())) {
                    skipped++;
                    continue;
                }

                EmergencyMessage entity = convertToEntity(dto);
                emergencyMessageRepository.save(entity);
                saved++;
            } catch (Exception e) {
                log.error("[재난문자] 저장 실패 - SN: {}", dto.getSerialNumber(), e);
                throw new ExceptionHandler(ErrorStatus._INTERNAL_SERVER_ERROR);
            }
        }

        log.info("[재난문자] 오늘 데이터 수집 완료 - 저장: {}, 중복 스킵: {}", saved, skipped);
    }

    private EmergencyMessage convertToEntity(EmergencyMessageDto dto) {
        LocalDate regDate = null;
        LocalDateTime createdAt = null;

        try {
            if (dto.getRegDate() != null) {
                regDate = LocalDate.parse(dto.getRegDate());
            }
            if (dto.getCreatedAt() != null) {
                createdAt = LocalDateTime.parse(dto.getCreatedAt(), CRT_DT_FORMATTER);
            }
        } catch (Exception e) {
            log.warn("[재난문자] 날짜 파싱 실패 - regDate: {}, createdAt: {}",
                    dto.getRegDate(), dto.getCreatedAt(), e);
        }

        return EmergencyMessage.builder()
                .serialNumber(dto.getSerialNumber())
                .messageContent(dto.getMessageContent())
                .regionName(dto.getRegionName())
                .createdAt(createdAt)
                .regDate(regDate)
                .stepName(dto.getStepName())
                .disasterTypeName(dto.getDisasterTypeName())
                .build();
    }

    // 5분마다 최신 재난문자 데이터 수집
    @Scheduled(cron = "0 0/5 * * * *")  // 매 5분 0초마다
    public void scheduledFetchTodayMessages() {
        try {
            fetchAndSaveTodayMessages();
        } catch (Exception e) {
            log.error("[재난문자] 스케줄링 수집 중 예외", e);
        }
    }
}
