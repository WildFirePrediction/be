package com.capstone25.WildFirePrediction.service;

import com.capstone25.WildFirePrediction.dto.EmergencyMessageDto;
import com.capstone25.WildFirePrediction.dto.response.PublicApiResponse;
import com.capstone25.WildFirePrediction.dto.response.PublicApiResponse.PagedResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmergencyMessageApiService {

    private final PublicApiService publicApiService;

    public PublicApiResponse.PagedResponse<EmergencyMessageDto> fetchPage(int pageNo) {
        return publicApiService.fetchPaged(
                "emergency-message",
                pageNo,
                new ParameterizedTypeReference<PagedResponse<EmergencyMessageDto>>() {}
        );
    }
}
