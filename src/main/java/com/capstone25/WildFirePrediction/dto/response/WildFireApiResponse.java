package com.capstone25.WildFirePrediction.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class WildFireApiResponse {

    private Header header;
    private int pageNo;
    private int numOfRows;
    @JsonProperty("totalCount")
    private int totalCount;
    private List<WildFireData> body;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Header {
        private String resultCode;
        private String resultMsg;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class WildFireData {
        @JsonProperty("FRSTFR_INFO_ID")
        private String wildFireInfoId;           // 산불정보아이디

        @JsonProperty("FRSTFR_GNT_DT")
        private String ignitionDateStr;          // 산불발화일시

        @JsonProperty("EXTNGS_CMPTN_DT")
        private String extinguishCompleteDateStr; // 진화완료일시

        @JsonProperty("FRSTFR_DCLR_ADDR")
        private String reportAddress;            // 산불신고주소

        @JsonProperty("FRSTFR_PSTN_XCRD")
        private String positionX;                // 산불위치X좌표

        @JsonProperty("FRSTFR_PSTN_YCRD")
        private String positionY;                // 산불위치Y좌표

        @JsonProperty("FRSTFR_GNT_PLC")
        private String ignitionPlace;            // 산불발화장소

        @JsonProperty("HKNG_CNTRL_YN")
        private String hikingControlYn;          // 등산통제여부

        @JsonProperty("FRSTFR_OCRN_CS_CLSF_CD")
        private String causeClassCode;           // 산불발생원인분류코드

        @JsonProperty("FRSTFR_OCRN_CS_DTL_CN")
        private String causeDetail;              // 산불발생원인상세내용

        @JsonProperty("FRSTFR_GRS_DAM_AMT")
        private String totalDamageAmount;        // 산불총피해금액

        @JsonProperty("FRSTFR_DCLR_YMD")
        private String reportDateStr;            // 산불신고일자

        @JsonProperty("STDG_CTPV_CD")
        private String sidoCode;                 // 법정동시도코드

        @JsonProperty("STDG_SGG_CD")
        private String sigunguCode;              // 법정동시군구코드

        @JsonProperty("GRS_FRSTFR_DAM_AREA")
        private String totalDamageArea;          // 총산불피해면적

        @JsonProperty("MAAS_OBTN_YMD")
        private String maasObtainDateStr;        // 행정안전부입수일자
    }
}
