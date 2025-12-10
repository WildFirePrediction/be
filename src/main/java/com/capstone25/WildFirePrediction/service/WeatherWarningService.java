package com.capstone25.WildFirePrediction.service;

import com.capstone25.WildFirePrediction.domain.WeatherWarning;
import com.capstone25.WildFirePrediction.dto.WeatherWarningDto;
import com.capstone25.WildFirePrediction.dto.response.PublicApiResponse.PagedResponse;
import com.capstone25.WildFirePrediction.repository.WeatherWarningRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class WeatherWarningService {
    private final WeatherWarningApiService weatherWarningApiService;
    private final WeatherWarningRepository weatherWarningRepository;

    private static final DateTimeFormatter PRSNTN_TM_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmm"); // "202312120130" 형식 가정

    // 중복 체크용 키 클래스
    record WarningKey(String title, LocalDate baseDate, LocalDateTime presentationTime) {}

    @Transactional
    public void fetchAndSaveTodayWarnings() {
        LocalDate today = LocalDate.now();
        log.info("[기상특보] 오늘 데이터 수집 시작 - {}", today);

        // 1. 오늘 특보 전체를 한 번에 조회 (N+1 해결)
        Set<WarningKey> existingKeys = weatherWarningRepository
                .findByBaseDateOrObtainedDate(today, today)
                .stream()
                .map(this::toWarningKey)
                .collect(Collectors.toSet());

        log.info("[기상특보] 기존 오늘 특보 {}건 메모리 로드 완료", existingKeys.size());

        int pageNo = 1;
        int maxPages = 5;  // 안전장치
        int saved = 0;
        int skipped = 0;
        int failed = 0;  // 실패 카운트

        while (pageNo <= maxPages) {
            PagedResponse<WeatherWarningDto> page = weatherWarningApiService.fetchPage(pageNo);

            if (page == null || page.getBody() == null || page.getBody().isEmpty()) {
                log.info("[기상특보] page {} 응답 비어 있음 → 중단", pageNo);
                break;
            }

            // 오늘 기준 필터 (기준일 or 취득일)
            List<WeatherWarningDto> todayWarnings = page.getBody().stream()
                    .filter(dto -> {
                        boolean baseMatch = today.toString().equals(dto.getBaseDate());
                        boolean obtainedMatch = today.toString().equals(dto.getObtainedDate());
                        return baseMatch || obtainedMatch;
                    })
                    .toList();

            if (todayWarnings.isEmpty()) {
                log.info("[기상특보] page {} 에 오늘 데이터 없음 → 중단", pageNo);
                break;
            }

            for (WeatherWarningDto dto : todayWarnings) {
                try {
                    WeatherWarning entity = convertToEntity(dto);
                    WarningKey key = toWarningKey(entity);

                    // 메모리에서 O(1) 체크
                    if (existingKeys.contains(key)) {
                        skipped++;
                        continue;
                    }

                    weatherWarningRepository.save(entity);
                    saved++;
                    existingKeys.add(key);  // 동적으로 업데이트
                } catch (Exception e) {
                    log.error("[기상특보] 저장 실패 - title: {}, 메시지: {}", dto.getTitle(), e.getMessage(), e);
                    failed++;   // 실패해도 계속 진행
                }
            }

            log.info("[기상특보] page {} 처리 완료 - 저장: {}, 스킵: {}, 실패: {}", pageNo, saved, skipped, failed);

            pageNo++;
        }

        log.info("[기상특보] 오늘 데이터 수집 종료 - 총 저장: {}, 중복 스킵: {}, 실패: {}", saved, skipped, failed);
    }

    // 키 변환 헬퍼
    private WarningKey toWarningKey(WeatherWarning entity) {
        return new WarningKey(
                entity.getTitle(),
                entity.getBaseDate(),
                entity.getPresentationTime()
        );
    }

    private WeatherWarning convertToEntity(WeatherWarningDto dto) {
        LocalDate baseDate = null;
        LocalDate obtainedDate = null;
        LocalDateTime prsTime = null;

        try {
            if (dto.getBaseDate() != null) {
                baseDate = LocalDate.parse(dto.getBaseDate());
            }
            if (dto.getObtainedDate() != null) {
                obtainedDate = LocalDate.parse(dto.getObtainedDate());
            }
            if (dto.getPresentationTime() != null && !dto.getPresentationTime().isBlank()) {
                prsTime = LocalDateTime.parse(dto.getPresentationTime().trim(), PRSNTN_TM_FORMATTER);
            }
        } catch (Exception e) {
            log.warn("[기상특보] 날짜/시간 파싱 실패 - baseDate: {}, obtainedDate: {}, prsTime: {}",
                    dto.getBaseDate(), dto.getObtainedDate(), dto.getPresentationTime(), e);
        }

        return WeatherWarning.builder()
                .title(dto.getTitle())
                .relevantZone(dto.getRelevantZone())
                .baseDate(baseDate)
                .presentationTime(prsTime)
                .currentAlertContent(dto.getCurrentAlertContent())
                .reservedAlertContent(dto.getReservedAlertContent())
                .presentationCode(dto.getPresentationCode())
                .obtainedDate(obtainedDate)
                .referenceMatter(dto.getReferenceMatter())
                .presentationContent(dto.getPresentationContent())
                .forecaster(dto.getForecaster())
                .build();
    }

    // 5분마다 최신 기상특보 데이터 수집
    @Scheduled(cron = "0 0/5 * * * *")  // 매 5분 0초마다
    public void scheduledFetchTodayWarnings() {
        try {
            fetchAndSaveTodayWarnings();
        } catch (Exception e) {
            log.error("[기상특보] 스케줄링 수집 중 예외", e);
        }
    }
}
