package com.capstone25.WildFirePrediction.service;

import com.capstone25.WildFirePrediction.domain.WeatherWarning;
import com.capstone25.WildFirePrediction.dto.WeatherWarningDto;
import com.capstone25.WildFirePrediction.dto.response.PublicApiResponse;
import com.capstone25.WildFirePrediction.global.code.status.ErrorStatus;
import com.capstone25.WildFirePrediction.global.exception.handler.ExceptionHandler;
import com.capstone25.WildFirePrediction.repository.WeatherWarningRepository;
import jakarta.transaction.Transactional;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class WeatherWarningService {
    private final WeatherWarningApiService weatherWarningApiService;
    private final WeatherWarningRepository weatherWarningRepository;

    private static final DateTimeFormatter PRSNTN_TM_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmm"); // "202312120130" 형식 가정

    @Transactional
    public void fetchAndSaveTodayWarnings() {
        LocalDate today = LocalDate.now();
        log.info("[기상특보] 오늘 데이터 수집 시작 - {}", today);

        PublicApiResponse.PagedResponse<WeatherWarningDto> page =
                weatherWarningApiService.fetchPage(1);

        if (page == null || page.getBody() == null || page.getBody().isEmpty()) {
            log.info("[기상특보] 오늘 API 응답 비어 있음");
            return;
        }

        List<WeatherWarningDto> todayWarnings = page.getBody().stream()
                .filter(dto -> {
                    // 기준일 또는 취득일 중 하나가 오늘인 것만 사용
                    boolean baseMatch = today.toString().equals(dto.getBaseDate());
                    boolean obtainedMatch = today.toString().equals(dto.getObtainedDate());
                    return baseMatch || obtainedMatch;
                })
                .toList();

        if (todayWarnings.isEmpty()) {
            log.info("[기상특보] 오늘 데이터 없음 (baseDate/obtainedDate 기준)");
            return;
        }

        int saved = 0;
        int skipped = 0;

        for (WeatherWarningDto dto : todayWarnings) {
            try {
                WeatherWarning entity = convertToEntity(dto);
                // 제목 + 기준일 + 발표시각 조합으로 중복 체크
                if (weatherWarningRepository.existsByTitleAndBaseDateAndPresentationTime(
                        entity.getTitle(), entity.getBaseDate(), entity.getPresentationTime())) {
                    skipped++;
                    continue;
                }

                weatherWarningRepository.save(entity);
                saved++;
            } catch (Exception e) {
                log.error("[기상특보] 저장 실패 - title: {}", dto.getTitle(), e);
                throw new ExceptionHandler(ErrorStatus._INTERNAL_SERVER_ERROR);
            }
        }

        log.info("[기상특보] 오늘 데이터 수집 완료 - 저장: {}, 중복 스킵: {}", saved, skipped);
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
                String trimmed = dto.getPresentationTime().trim();
                prsTime = LocalDateTime.parse(trimmed, PRSNTN_TM_FORMATTER);
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
