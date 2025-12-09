package com.capstone25.WildFirePrediction.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true) // 추후 추가 필드 무시
public class ShelterApiResponse {

    private Header header;
    private Body body;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Header {
        private String resultCode;
        private String resultMsg;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Body {
        private int pageNo;
        private int totalCount;
        private int numOfRows;
        private List<ShelterData> data;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ShelterData {

        @JsonProperty("REARE_NM")
        private String facilityName;

        @JsonProperty("RONA_DADDR")
        private String roadAddress;

        @JsonProperty("LAT")
        private String latitude;

        @JsonProperty("LOT")
        private String longitude;

        @JsonProperty("SHLT_SE_CD")
        private String shelterTypeCode;

        @JsonProperty("SHLT_SE_NM")
        private String shelterTypeName;

        @JsonProperty("MNG_SN")
        private String managementNumber;
    }
}
