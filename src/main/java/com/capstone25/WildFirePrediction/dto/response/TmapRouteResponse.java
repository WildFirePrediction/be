package com.capstone25.WildFirePrediction.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class TmapRouteResponse {
    private String type; // "FeatureCollection"
    private List<Feature> features;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Feature {
        private String type; // "Feature"
        private Geometry geometry;
        private Properties properties;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Geometry {
        private String type; // "Point" or "LineString"
        private Object coordinates; // List<Double> or List<List<Double>>
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Properties {
        private Integer index;
        private Integer totalDistance;
        private Integer totalTime;
        private String pointType; // "SP", "EP", "GP"
        private Integer distance;
        private Integer time;
        private String description;
    }
}
