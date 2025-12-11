package com.capstone25.WildFirePrediction.util;

import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class WeatherRegionParser {

    // effective_status_content 전체에서 시/도 이름만 추출
    public List<String> extractSidoList(String effectiveStatusContent) {
        List<String> result = new ArrayList<>();
        if (effectiveStatusContent == null || effectiveStatusContent.isBlank()) {
            return result;
        }

        // 줄 단위 분리
        String[] lines = effectiveStatusContent.split("\\r?\\n");
        for (String line : lines) {
            line = line.trim();
            if (!line.startsWith("o")) {
                continue; // "o ..." 형식만 대상
            }

            // 바다/해역 관련 키워드 포함된 줄은 건너뜀 (풍랑주의보, 앞바다, 먼바다, 전해상 등)
            if (isSeaLine(line)) {
                continue;
            }

            int colonIdx = line.indexOf(':');
            if (colonIdx == -1) continue;

            // 콜론 뒤 지역 부분만
            String regionPart = line.substring(colonIdx + 1).trim();
            // 쉼표 기준 분리
            String[] tokens = regionPart.split(",");
            for (String token : tokens) {
                String raw = token.trim();
                if (raw.isEmpty()) continue;

                // 괄호 앞까지만 시/도 후보로 사용
                String sidoCandidate = raw.split("\\(")[0].trim();
                if (sidoCandidate.isEmpty()) continue;

                String normalized = normalizeSidoName(sidoCandidate);
                result.add(normalized);
            }
        }

        return result;
    }

    // 바다/해역 줄인지 판별
    private boolean isSeaLine(String line) {
        return line.contains("앞바다")
                || line.contains("먼바다")
                || line.contains("전해상")
                || line.contains("안쪽먼바다")
                || line.contains("바깥먼바다")
                || line.contains("해상");
    }

    // 시/도 이름 보정
    private String normalizeSidoName(String name) {
        if (name.endsWith("도")) return name;
        if (name.endsWith("광역시") || name.endsWith("특별시") || name.endsWith("자치도")) return name;

        return switch (name) {
            case "부산" -> "부산광역시";
            case "울산" -> "울산광역시";
            case "대구" -> "대구광역시";
            case "인천" -> "인천광역시";
            case "광주" -> "광주광역시";
            case "대전" -> "대전광역시";
            case "세종" -> "세종특별자치시";
            case "제주" -> "제주특별자치도";
            case "경기" -> "경기도";
            case "강원" -> "강원특별자치도";
            case "충북" -> "충청북도";
            case "충남" -> "충청남도";
            case "전북" -> "전라북도";
            case "전남" -> "전라남도";
            case "경북" -> "경상북도";
            case "경남" -> "경상남도";
            case "서울" -> "서울특별시";
            default -> name;
        };
    }
}
