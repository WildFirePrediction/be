package com.capstone25.WildFirePrediction.service;

import com.capstone25.WildFirePrediction.domain.WeatherWarning;
import com.capstone25.WildFirePrediction.dto.response.WeatherWarningApiResponse;
import com.capstone25.WildFirePrediction.global.code.status.ErrorStatus;
import com.capstone25.WildFirePrediction.global.exception.handler.ExceptionHandler;
import com.capstone25.WildFirePrediction.repository.WeatherWarningRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class WeatherWarningService {

    private final WeatherWarningApiService weatherWarningApiService;
    private final WeatherWarningRepository weatherWarningRepository;

    private static final DateTimeFormatter PRSNTN_TM_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmm");
    private static final DateTimeFormatter INQ_DT_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    // 원시 JSON 조회
    public String loadWeatherWarningRaw(String dateStr) {
        String targetDate = (dateStr == null || dateStr.isBlank())
                ? LocalDate.now().format(INQ_DT_FORMATTER)
                : dateStr.trim();

        log.info("[기상특보] 원시 조회 시작 - inqDt: {}", targetDate);
        try {
            String rawJson = weatherWarningApiService.fetchWeatherWarningRaw(1, targetDate);
            log.info("[기상특보] 원시 응답 길이: {}", rawJson != null ? rawJson.length() : 0);
            return rawJson;
        } catch (Exception e) {
            log.error("[기상특보] 원시 조회 실패 - inqDt: {}", targetDate, e);
            return "{ \"error\": \"" + e.getMessage() + "\" }";
        }
    }

    // (임시) 오늘자 특보 동기화 – 필요하면 inqDt 기반으로 확장
    public void loadTodaysWeatherWarnings() {
        String today = LocalDate.now().format(INQ_DT_FORMATTER);
        loadWeatherWarningsByDate(today);
    }

    public void loadWeatherWarningsByDate(String dateStr) {
        try {
            int pageNo = 1;
            int totalSavedCount = 0;
            int totalPages = 1; // 첫 페이지 조회 후 실제 값으로 업데이트

            do {
                WeatherWarningApiResponse response =
                        weatherWarningApiService.fetchWeatherWarningPage(pageNo, dateStr);

                if (response == null || response.getBody() == null || response.getBody().isEmpty()) {
                    log.info("[기상특보] page {}에서 수집할 데이터가 없습니다. inqDt={}", pageNo, dateStr);
                    break;
                }

                if (pageNo == 1) {
                    int totalCount = response.getTotalCount();
                    int pageSize = response.getNumOfRows();
                    log.info("[기상특보] API 응답 - totalCount: {}, numOfRows: {}, 실제 데이터: {}건",
                            totalCount, pageSize, response.getBody().size());
                    if (pageSize > 0) {
                        totalPages = (int) Math.ceil((double) totalCount / pageSize);
                    }
                }

                int savedCount = saveWeatherWarnings(response);
                totalSavedCount += savedCount;
                log.info("[기상특보] Page {}/{} 동기화 완료 - 신규: {}건", pageNo, totalPages, savedCount);

                pageNo++;
            } while (pageNo <= totalPages);

            log.info("[기상특보] 전체 동기화 완료 - inqDt={}, 총 신규: {}건", dateStr, totalSavedCount);
        } catch (Exception e) {
            log.error("[기상특보] 동기화 실패 - inqDt={}", dateStr, e);
            throw new ExceptionHandler(ErrorStatus.WEATHER_WARNING_DATA_LOAD_FAILED);
        }
    }

    private WeatherWarning convertToEntity(WeatherWarningApiResponse.WeatherWarningData data) {
        LocalDateTime presentationTime = LocalDateTime.parse(
                data.getPresentationTimeStr(), PRSNTN_TM_FORMATTER);

        return WeatherWarning.builder()
                .branch(data.getBranch())
                .presentationTime(presentationTime)
                .presentationSerial(data.getPresentationSerial())
                .forecasterName(data.getForecasterName())
                .warningPresentationCode(data.getWarningPresentationCode())
                .title(data.getTitle())
                .relevantZone(data.getRelevantZone())
                .effectiveTimeText(data.getEffectiveTimeText())
                .content(data.getContent())
                .effectiveStatusTimeRaw(data.getEffectiveStatusTimeRaw())
                .effectiveStatusContent(data.getEffectiveStatusContent())
                .reservedWarningStatus(data.getReservedWarningStatus())
                .referenceMatter(data.getReferenceMatter())
                .maasObtainedAtRaw(data.getMaasObtainedAtRaw())
                .build();
    }

    // API 응답 → 엔티티 변환 + 저장 (중복 체크)
    @Transactional
    public int saveWeatherWarnings(WeatherWarningApiResponse apiResponse) {
        if (apiResponse.getBody() == null || apiResponse.getBody().isEmpty()) {
            log.info("[기상특보] 저장할 데이터 없음");
            return 0;
        }

        int savedCount = 0;
        for (WeatherWarningApiResponse.WeatherWarningData data : apiResponse.getBody()) {
            try {
                WeatherWarning warning = convertToEntity(data);
                weatherWarningRepository.save(warning);
                savedCount++;
            } catch (DataIntegrityViolationException e) {
                // unique 제약 위반 → 이미 저장된 데이터이므로 정상적인 중복 상황
                log.trace("[기상특보] 중복 데이터 건너뜀: {}", data.getTitle());
            } catch (Exception e) {
                log.error("[기상특보] 저장 실패: {}", data.getTitle(), e);
            }
        }
        log.info("[기상특보] {}건 저장 완료", savedCount);
        return savedCount;
    }

    // 5분마다 최신 통보문 조회
    @Scheduled(cron = "0 0/5 * * * *")
    @Transactional
    public void scheduledFetchWeatherWarnings() {
        log.info("[기상특보] 5분 주기 수집 시작");
        try {
            loadTodaysWeatherWarnings();
            log.info("[기상특보] 5분 주기 수집 완료");
        } catch (Exception e) {
            log.error("[기상특보] 스케줄링 수집 중 예외 발생", e);
            // 다음 실행까지 계속 동작 보장
        }
    }
}
