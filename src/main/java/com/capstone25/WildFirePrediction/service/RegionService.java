package com.capstone25.WildFirePrediction.service;

import com.capstone25.WildFirePrediction.domain.EmergencyMessage;
import com.capstone25.WildFirePrediction.domain.Region;
import com.capstone25.WildFirePrediction.dto.response.RegionResponse;
import com.capstone25.WildFirePrediction.repository.EmergencyMessageRepository;
import com.capstone25.WildFirePrediction.repository.RegionRepository;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class RegionService {

    private final RegionRepository regionRepository;
    private final EmergencyMessageRepository emergencyMessageRepository;

    @Transactional
    public int importRegionsFromCsv(MultipartFile file) throws Exception {
        // 1. DB에서 모든 adminCode를 한 번에 조회하여 Set에 저장
        java.util.Set<String> existingAdminCodes = regionRepository.findAll().stream()
                .map(Region::getAdminCode)
                .collect(java.util.stream.Collectors.toSet());

        List<Region> regions = new ArrayList<>();

        try(CSVReader reader = new CSVReaderBuilder(
                new InputStreamReader(file.getInputStream(), Charset.forName("MS949"))
        )
                .withSkipLines(1)   // 첫 줄 헤더 스킵
                .build()
        ) {
            String[] line;
            while ((line = reader.readNext()) != null) {
                // CSV: 0=행정기관코드, 1=시도명, 2=시군구명, 3=읍면동명 ...
                if (line.length < 4)    continue;

                String adminCode = line[0].trim();

                // 2. DB 쿼리 대신 메모리에서 존재 여부 확인
                if (existingAdminCodes.contains(adminCode)) {
                    continue;
                }

                String sido = line[1].trim();
                String sigungu = line[2].trim();
                String eupmyeondong = line[3].trim();

                Region region = Region.builder()
                        .adminCode(adminCode)
                        .sido(sido)
                        .sigungu(sigungu)
                        .eupmyeondong(eupmyeondong)
                        .build();

                regions.add(region);
                existingAdminCodes.add(adminCode); // 동일 파일 내 중복 레코드 방지
            }
        }

        regionRepository.saveAll(regions);
        return regions.size();
    }

    // 지역별 재난문자 조회
    @Transactional(readOnly = true)
    public RegionResponse.RegionDisasterDto getDisastersByRegionId(Long regionId) {
        Region region = regionRepository.findById(regionId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 지역 ID: " + regionId));

        // 당일 재난문자 ID들 가져오기
        List<Long> emergencyIds = region.getEmergencyMessageIds();

        // 해당 ID들의 재난문자 조회 (IN 절, N+1 방지)
        List<EmergencyMessage> emergencies = emergencyMessageRepository.findAllById(emergencyIds);

        // 최신순 정렬
        emergencies.sort(Comparator.comparing(EmergencyMessage::getCreatedAt).reversed());

        return RegionResponse.RegionDisasterDto.builder()
                .region(toRegionDto(region))
                .emergencyMessages(emergencies)
                .build();
    }

    // Region 엔티티를 RegionResponseDto로 변환
    private RegionResponse.RegionResponseDto toRegionDto(Region region) {
        return new RegionResponse.RegionResponseDto(
                region.getId(),
                region.getSido(),
                region.getSigungu(),
                region.getEupmyeondong()
        );
    }
}
