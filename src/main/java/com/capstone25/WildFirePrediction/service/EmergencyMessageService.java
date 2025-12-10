package com.capstone25.WildFirePrediction.service;

import com.capstone25.WildFirePrediction.domain.EmergencyMessage;
import com.capstone25.WildFirePrediction.dto.EmergencyMessageDto;
import com.capstone25.WildFirePrediction.dto.response.PublicApiResponse.PagedResponse;
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
        Long lastSn = emergencyMessageRepository.findTopByOrderBySerialNumberDesc()
                .map(EmergencyMessage::getSerialNumber)
                .orElse(0L);    // 마지막 저장된 SN 조회

        log.info("[재난문자] 신규 데이터 수집 시작 - today: {}, lastSn: {}", today, lastSn);

        int pageNo = 1;
        int maxPages = 10;  // 안전장치
        int saved = 0;
        int skipped = 0;
        int failed = 0;  // 실패 카운트

        while (pageNo <= maxPages) {
            PagedResponse<EmergencyMessageDto> page = emergencyMessageApiService.fetchPage(pageNo);

            if (page == null || page.getBody() == null || page.getBody().isEmpty()) {
                log.info("[재난문자] page {} 응답 비어 있음 → 중단", pageNo);
                break;
            }

            // 1) 오늘 데이터만, 2) lastSn 이후 것만
            List<EmergencyMessageDto> candidates = page.getBody().stream()
                    .filter(dto -> dto.getSerialNumber() != null)
                    .filter(dto -> today.toString().equals(dto.getRegDate()))
                    .filter(dto -> dto.getSerialNumber() > lastSn)
                    .toList();

            if (candidates.isEmpty()) {
                log.info("[재난문자] page {} 에서 신규(today & SN>{}) 데이터 없음 → 중단", pageNo, lastSn);
                break;
            }

            for (EmergencyMessageDto dto : candidates) {
                try {
                    if (emergencyMessageRepository.existsBySerialNumber(dto.getSerialNumber())) {
                        skipped++;
                        continue;
                    }

                    EmergencyMessage entity = convertToEntity(dto);
                    emergencyMessageRepository.save(entity);
                    saved++;
                } catch (Exception e) {
                    log.error("[재난문자] 저장 실패 - SN: {}, 메시지: {}", dto.getSerialNumber(), e.getMessage(), e);
                    failed++;   // 실패해도 계속 진행
                }
            }

            log.info("[재난문자] page {} 처리 완료 - 저장: {}, 스킵: {}, 실패: {}", pageNo, saved, skipped, failed);

            pageNo++;
        }

        log.info("[재난문자] 신규 데이터 수집 종료 - 총 저장: {}, 중복 스킵: {}, 실패: {}", saved, skipped, failed);
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
