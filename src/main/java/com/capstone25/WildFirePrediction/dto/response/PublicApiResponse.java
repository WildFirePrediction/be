package com.capstone25.WildFirePrediction.dto.response;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class PublicApiResponse {

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Header {
        private String resultMsg;   // "NORMAL SERVICE"
        private String resultCode;  // "00"이면 정상
        private String errorMsg;    // 에러 메시지 (없으면 null)
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PagedResponse<T> {
        private Header header;     // resultCode, resultMsg 등
        private int numOfRows;     // 페이지당 개수
        private int pageNo;        // 현재 페이지 번호
        private int totalCount;    // 총 데이터 수
        private List<T> body;      // 실제 데이터 리스트
    }
}
