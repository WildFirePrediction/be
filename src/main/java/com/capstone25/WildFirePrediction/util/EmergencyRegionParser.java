package com.capstone25.WildFirePrediction.util;

import java.util.Arrays;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class EmergencyRegionParser {

    // "경기도 수원시 장안구 ,경기도 수원시 권선구" → ["경기도 수원시 장안구", "경기도 수원시 권선구"]
    public List<String> splitRegionNames(String regionName) {
        if (regionName == null || regionName.isBlank()) {
            return List.of();
        }
        return Arrays.stream(regionName.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    // "경기도 수원시 장안구" → sido="경기도", sigungu="수원시 장안구"
    public ParsedRegion parseOne(String full) {
        String[] parts = full.split("\\s+");
        if (parts.length < 2) {
            return null;
        }
        String sido = parts[0];
        String sigungu = String.join(" ", Arrays.copyOfRange(parts, 1, parts.length));
        return new ParsedRegion(sido, sigungu);
    }

    public record ParsedRegion(String sido, String sigungu) {}
}
