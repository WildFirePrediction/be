package com.capstone25.WildFirePrediction.service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class WeatherWarningService {

    private final WeatherWarningApiService weatherWarningApiService;

    public String loadWeatherWarningRaw(String dateStr) {
        String targetDate = (dateStr == null || dateStr.isBlank())
                ? LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
                : dateStr.trim();

        log.info("기상특보 통보문 원시 조회 시작 - inqDt: {}", targetDate);

        String rawJson = weatherWarningApiService.fetchWeatherWarningRaw(1, targetDate);

        log.info("기상특보 통보문 원시 응답 길이: {}",
                rawJson != null ? rawJson.length() : 0);

        return rawJson;
    }
}
