package com.capstone25.WildFirePrediction.service;

import com.capstone25.WildFirePrediction.domain.Region;
import com.capstone25.WildFirePrediction.repository.RegionRepository;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class RegionCsvService {

    private final RegionRepository regionRepository;

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
}
