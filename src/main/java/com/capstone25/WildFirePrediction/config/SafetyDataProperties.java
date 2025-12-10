package com.capstone25.WildFirePrediction.config;

import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "safety-data")
public class SafetyDataProperties {

    private String baseUrl;  // https://www.safetydata.go.kr
    private Map<String, ApiConfig> apis;  // 각 API 설정을 Map으로 관리

    @Getter
    @Setter
    public static class ApiConfig {
        private String path;
        private String serviceKey;
        private int pageSize;
        private String formatParamName = "type";  // 기본값 "type"
    }
}
