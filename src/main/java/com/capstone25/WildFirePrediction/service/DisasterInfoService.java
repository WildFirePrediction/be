package com.capstone25.WildFirePrediction.service;

import com.capstone25.WildFirePrediction.domain.Earthquake;
import com.capstone25.WildFirePrediction.domain.WildFire;
import com.capstone25.WildFirePrediction.dto.response.EarthquakeApiResponse;
import com.capstone25.WildFirePrediction.dto.response.WildFireApiResponse;
import com.capstone25.WildFirePrediction.repository.EarthquakeRepository;
import com.capstone25.WildFirePrediction.repository.WildFireRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class DisasterInfoService {
    private final DisasterInfoApiService disasterInfoApiService;
    private final WildFireRepository wildFireRepository;
    private final EarthquakeRepository earthquakeRepository;

    private static final DateTimeFormatter GNT_DT_FORMATTER = DateTimeFormatter.ofPattern(
            "yyyy/MM/dd HH:mm:ss.SSSSSSSSS");
    private static final DateTimeFormatter MAAS_DT_FORMATTER = DateTimeFormatter.ofPattern(
            "yyyy/MM/dd HH:mm:ss.SSSSSSSSS");

    private static final DateTimeFormatter EQ_TIME_FORMATTER = DateTimeFormatter.ofPattern(
            "yyyy/MM/dd HH:mm:ss.SSSSSSSSS");
    private static final DateTimeFormatter MAAS_EQ_DT_FORMATTER = DateTimeFormatter.ofPattern(
            "yyyy/MM/dd HH:mm:ss.SSSSSSSSS");

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

    // startDt 기준으로 산불 재난정보 저장
    @Transactional
    public String loadAndSaveWildfiresByDate(String dateStr) {
        log.info("[산불] 동기화 시작 - startDt={}", dateStr);

        try {
            // 1. 첫 페이지 호출로 totalCount / numOfRows 확인
            WildFireApiResponse firstPage = disasterInfoApiService.fetchWildFirePage(1, dateStr);

            int totalCount = firstPage.getTotalCount();
            int pageSize = firstPage.getNumOfRows();
            int totalPages = (int) Math.ceil((double) totalCount / pageSize);

            log.info("[산불] totalCount={}, pageSize={}, totalPages={}",
                    totalCount, pageSize, totalPages);

            if (totalCount == 0) {
                return buildResultJson(0, 0, 0);
            }

            // 2. 첫 페이지의 FRSTFR_INFO_ID들로 기존 DB 확인
            Set<String> firstIds = new HashSet<>();
            if (firstPage.getBody() != null) {
                firstPage.getBody().forEach(d -> firstIds.add(d.getWildFireInfoId()));
            }
            Set<String> existingIds = wildFireRepository.findExistingIds(firstIds);
            log.info("[산불] 첫 페이지 기준 DB 기존 건수: {}", existingIds.size());

            int savedCount = 0;
            int skippedCount = 0;

            // 3. 페이지 루프
            for (int pageNo = 1; pageNo <= totalPages; pageNo++) {
                WildFireApiResponse pageResponse = (pageNo == 1)
                        ? firstPage
                        : disasterInfoApiService.fetchWildFirePage(pageNo, dateStr);

                if (pageResponse.getBody() == null || pageResponse.getBody().isEmpty()) {
                    log.info("[산불] 페이지 {} 데이터 없음", pageNo);
                    continue;
                }

                List<WildFire> entitiesToSave = new ArrayList<>();
                for (WildFireApiResponse.WildFireData data : pageResponse.getBody()) {
                    String infoId = data.getWildFireInfoId();
                    if (infoId == null || existingIds.contains(infoId)) {
                        skippedCount++;
                        continue;
                    }

                    try {
                        WildFire entity = convertToEntity(data);
                        entitiesToSave.add(entity);
                        existingIds.add(infoId);
                    } catch (Exception e) {
                        log.error("[산불] 변환 실패 - infoId={}", infoId, e);
                        skippedCount++;
                    }
                }

                if (!entitiesToSave.isEmpty()) {
                    wildFireRepository.saveAll(entitiesToSave);
                    savedCount += entitiesToSave.size();
                }

                log.info("[산불] 페이지 {} 저장: {}건 (누적 저장: {}, 누적 스킵: {})",
                        pageNo, entitiesToSave.size(), savedCount, skippedCount);
            }

            return buildResultJson(totalCount, savedCount, skippedCount);

        } catch (Exception e) {
            log.error("[산불] 동기화 실패 - startDt={}", dateStr, e);
            return "{ \"error\": \"" + e.getMessage() + "\" }";
        }
    }

    private WildFire convertToEntity(WildFireApiResponse.WildFireData data) {
        LocalDateTime ignition = parseDateTime(data.getIgnitionDateStr(), GNT_DT_FORMATTER);
        LocalDateTime maasObtained = parseDateTime(data.getMaasObtainDateStr(), MAAS_DT_FORMATTER);

        Double x = parseDoubleOrNull(data.getPositionX());
        Double y = parseDoubleOrNull(data.getPositionY());
        Double damageArea = parseDoubleOrNull(data.getTotalDamageArea());
        Long damageAmount = parseLongOrNull(data.getTotalDamageAmount());

        return WildFire.builder()
                .frstfrInfoId(data.getWildFireInfoId())
                .ignitionDateTime(ignition)
                .address(data.getReportAddress())
                .x(x)
                .y(y)
                .sidoCode(trimOrNull(data.getSidoCode()))
                .sigunguCode(trimOrNull(data.getSigunguCode()))
                .damageArea(damageArea)
                .damageAmount(damageAmount)
                .maasObtainedAt(maasObtained)
                .build();
    }

    private LocalDateTime parseDateTime(String str, DateTimeFormatter formatter) {
        if (str == null || str.isBlank())
            return null;
        String trimmed = str.trim();
        // "2025/12/11 18:12:34.000000000" 형식 → 공공 API 패턴과 맞게 파싱
        return LocalDateTime.parse(trimmed, formatter);
    }

    private Double parseDoubleOrNull(String v) {
        try {
            if (v == null || v.isBlank())
                return null;
            return Double.parseDouble(v.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Long parseLongOrNull(String v) {
        try {
            if (v == null || v.isBlank())
                return null;
            return Long.parseLong(v.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String trimOrNull(String v) {
        return (v == null) ? null : v.trim();
    }

    // 정리 결과 JSON (총 개수 / 저장 / 스킵)
    private String buildResultJson(int totalCount, int saved, int skipped) throws Exception {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalCountFromApi", totalCount);
        result.put("savedCount", saved);
        result.put("skippedCount", skipped);

        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        return mapper.writeValueAsString(result);
    }

    // 저장된 모든 산불 재난정보 조회
    public String getAllWildfires() {
        log.info("저장된 모든 산불 재난정보 조회 시작");

        try {
            List<WildFire> wildFires = wildFireRepository.findAll();
            log.info("총 {}건 조회됨", wildFires.size());

            // JSON으로 변환해서 리턴
            ObjectMapper mapper = new ObjectMapper();
            mapper.enable(SerializationFeature.INDENT_OUTPUT);

            return mapper.writeValueAsString(wildFires);

        } catch (Exception e) {
            log.error("산불 재난정보 조회 실패", e);
            return "{ \"error\": \"" + e.getMessage() + "\" }";
        }
    }


    // 원시 지진 재난정보 조회 (테스트용)
    public String loadRawEarthquakeMessages(int pageNo) {
        log.info("원시 지진 재난정보 조회 시작");

        try {
            EarthquakeApiResponse response = disasterInfoApiService.fetchEarthquakePage(pageNo);

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

    // 지진 수집 및 저장
    @Transactional
    public String loadAndSaveRecentEarthquakesKoreaOnly() {
        log.info("[지진] 동기화 시작 - 최신 1~11 페이지, 한국 영역만");

        try {
            int maxPage = 11;
            int savedCount = 0;
            int skippedCount = 0;
            int totalCountFromApi = 0;

            // 1페이지 먼저 호출해서 totalCount 확인 (전체 통계 용)
            EarthquakeApiResponse firstPage = disasterInfoApiService.fetchEarthquakePage(1);
            totalCountFromApi = firstPage.getTotalCount();

            // 첫 페이지의 ERQK_NO들로 기존 DB 확인
            Set<String> firstNos = new HashSet<>();
            if (firstPage.getBody() != null) {
                firstPage.getBody().forEach(d -> firstNos.add(d.getEarthquakeNo()));
            }
            Set<String> existingNos = earthquakeRepository.findExistingNos(firstNos);
            log.info("[지진] 첫 페이지 기준 DB 기존 건수: {}", existingNos.size());

            for (int pageNo = 1; pageNo <= maxPage; pageNo++) {
                EarthquakeApiResponse page = (pageNo == 1)
                        ? firstPage
                        : disasterInfoApiService.fetchEarthquakePage(pageNo);

                if (page.getBody() == null || page.getBody().isEmpty()) {
                    log.info("[지진] 페이지 {} 데이터 없음", pageNo);
                    continue;
                }

                List<Earthquake> toSave = new ArrayList<>();

                for (EarthquakeApiResponse.EarthquakeData data : page.getBody()) {
                    String eqNo = data.getEarthquakeNo();
                    if (eqNo == null || existingNos.contains(eqNo)) {
                        skippedCount++;
                        continue;
                    }

                    // 좌표 파싱
                    Double lat = parseDoubleOrNull(data.getLatitude());
                    Double lon = parseDoubleOrNull(data.getLongitude());
                    if (lat == null || lon == null) {
                        skippedCount++;
                        continue;
                    }

                    // 한국 영역만
                    if (!isKorea(lat, lon)) {
                        skippedCount++;
                        continue;
                    }

                    try {
                        Earthquake entity = convertToEarthquakeEntity(data, lat, lon);
                        toSave.add(entity);
                        existingNos.add(eqNo);
                    } catch (Exception e) {
                        log.error("[지진] 변환 실패 - eqNo={}", eqNo, e);
                        skippedCount++;
                    }
                }

                if (!toSave.isEmpty()) {
                    earthquakeRepository.saveAll(toSave);
                    savedCount += toSave.size();
                }

                log.info("[지진] 페이지 {} 저장: {}건 (누적 저장: {}, 누적 스킵: {})",
                        pageNo, toSave.size(), savedCount, skippedCount);
            }

            return buildEqResultJson(totalCountFromApi, savedCount, skippedCount);

        } catch (Exception e) {
            log.error("[지진] 동기화 실패", e);
            return "{ \"error\": \"" + e.getMessage() + "\" }";
        }
    }

    private boolean isKorea(double lat, double lon) {
        // 한반도 주변 넉넉한 범위
        return lat >= 32.0 && lat <= 44.0
                && lon >= 122.0 && lon <= 134.0;
    }

    private Earthquake convertToEarthquakeEntity(EarthquakeApiResponse.EarthquakeData data,
                                                 Double lat, Double lon) {
        // 발생시각은 API에서 어떤 필드를 기준으로 쓸지 선택
        // 여기서는 PRSNTN_TM(발표시각)이 "20251211235900" 같은 형태라고 가정하면
        LocalDateTime occurrence = parseEqTime(data.getPresentationTimeStr(), EQ_TIME_FORMATTER);

        LocalDateTime maasObtained = parseEqTime(data.getMaasObtainDateStr(), MAAS_EQ_DT_FORMATTER);

        Double scale = parseDoubleOrNull(data.getScale());
        Double depthKm = parseDoubleOrNull(data.getOccurrenceDepth());

        return Earthquake.builder()
                .earthquakeNo(data.getEarthquakeNo())
                .branchNo(data.getBranchNo())
                .disasterTypeKind(data.getDisasterTypeKind())
                .occurrenceTime(occurrence)
                .latitude(lat)
                .longitude(lon)
                .position(data.getPosition())
                .scale(scale)
                .depthKm(depthKm)
                .notificationLevel(data.getNotificationLevel())
                .refNo(data.getRefNo())
                .refMatter(data.getRefMatter())
                .modificationMatter(data.getModificationMatter())
                .maasObtainedAt(maasObtained)
                .build();
    }

    private LocalDateTime parseEqTime(String str, DateTimeFormatter formatter) {
        if (str == null || str.isBlank()) return null;
        String trimmed = str.trim();
        return LocalDateTime.parse(trimmed, formatter);
    }

    private String buildEqResultJson(int totalCount, int saved, int skipped) throws Exception {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalCountFromApi", totalCount);
        result.put("savedCount", saved);
        result.put("skippedCount", skipped);

        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        return mapper.writeValueAsString(result);
    }

    // 저장된 모든 지진 재난정보 조회
    public String getAllEarthquakes() {
        log.info("저장된 모든 지진 재난정보 조회 시작");
        try {
            List<Earthquake> eqs = earthquakeRepository.findAll();
            log.info("총 {}건 조회됨", eqs.size());
            ObjectMapper mapper = new ObjectMapper();
            mapper.enable(SerializationFeature.INDENT_OUTPUT);
            return mapper.writeValueAsString(eqs);
        } catch (Exception e) {
            log.error("지진 재난정보 조회 실패", e);
            return "{ \"error\": \"" + e.getMessage() + "\" }";
        }
    }

}
