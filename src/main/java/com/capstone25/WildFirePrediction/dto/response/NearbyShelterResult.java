package com.capstone25.WildFirePrediction.dto.response;

import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class NearbyShelterResult {
    private List<ShelterResponse> shelters;
    private int radiusKm;        // 3km에서 찾음
    private int page;            // 현재 페이지 (0부터)
    private int size;            // 페이지당 개수
    private long totalCount;     // 전체 근처 대피소 수
    private boolean hasMore;     // 더보기 버튼 표시 여부
}
