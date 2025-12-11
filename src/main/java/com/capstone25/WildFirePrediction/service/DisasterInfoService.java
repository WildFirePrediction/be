package com.capstone25.WildFirePrediction.service;

import com.capstone25.WildFirePrediction.dto.response.EarthquakeApiResponse;
import com.capstone25.WildFirePrediction.dto.response.WildFireApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class DisasterInfoService {
    private final DisasterInfoApiService disasterInfoApiService;

    // 원시 산불 재난정보 조회 (테스트용)
    public String loadRawWildfireMessages(String date) {
        log.info("원시 산불 재난정보 조회 시작 - startDt: {}", date);

        try {
            WildFireApiResponse response = disasterInfoApiService.fetchWildFirePage(1, date);

            int totalCount = response.getTotalCount();
            int pageSize = response.getNumOfRows();

            log.info("API 응답 - totalCount: {}, numOfRows: {}, 실제 데이터: {}건",
                    totalCount, pageSize,
                    response.getBody() != null ? response.getBody().size() : 0);

            // JSON으로 변환해서 리턴
            ObjectMapper mapper = new ObjectMapper();
            mapper.enable(SerializationFeature.INDENT_OUTPUT);

            return mapper.writeValueAsString(response);

        } catch (Exception e) {
            log.error("원시 산불 조회 실패 - startDt: {}", date, e);
            return "{ \"error\": \"" + e.getMessage() + "\" }";
        }
    }

    // 원시 지진 재난정보 조회 (테스트용)
    public String loadRawEarthquakeMessages() {
        log.info("원시 지진 재난정보 조회 시작");

        try {
            EarthquakeApiResponse response = disasterInfoApiService.fetchEarthquakePage(1);

            int totalCount = response.getTotalCount();
            int pageSize = response.getNumOfRows();

            log.info("API 응답 - totalCount: {}, numOfRows: {}, 실제 데이터: {}건",
                    totalCount, pageSize,
                    response.getBody() != null ? response.getBody().size() : 0);

            // JSON으로 변환해서 리턴
            ObjectMapper mapper = new ObjectMapper();
            mapper.enable(SerializationFeature.INDENT_OUTPUT);

            return mapper.writeValueAsString(response);

        } catch (Exception e) {
            log.error("원시 지진 조회 실패", e);
            return "{ \"error\": \"" + e.getMessage() + "\" }";
        }
    }
}
