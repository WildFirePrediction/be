package com.capstone25.WildFirePrediction.service;

import com.capstone25.WildFirePrediction.dto.response.PublicApiResponse;
import com.capstone25.WildFirePrediction.dto.ShelterDto;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ShelterApiService {

    private final PublicApiService publicApiService;

    // api에서 대피소 정보 한 페이지 조회 (페이징)
    public PublicApiResponse.PagedResponse<ShelterDto> fetchShelterPage(int pageNo) {
        // 대피소 API 호출
        PublicApiResponse.PagedResponse<ShelterDto> response =
                publicApiService.fetchPaged(
                        "shelter",
                        pageNo,
                        new ParameterizedTypeReference<PublicApiResponse.PagedResponse<ShelterDto>>() {
                        }
                );

        // API 응답 검증
        if (response == null) {
            log.warn("대피소 API 응답이 null입니다 - 페이지: {}", pageNo);
            return null;
        }

        List<ShelterDto> dataList = response.getBody();
        log.info("대피소 API 호출 성공 - 총 {}건 중 페이지 {}: {}건",
                response.getTotalCount(), pageNo, dataList != null ? dataList.size() : 0);

        return response;
    }
}
