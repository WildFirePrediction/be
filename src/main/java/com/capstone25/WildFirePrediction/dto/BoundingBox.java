package com.capstone25.WildFirePrediction.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BoundingBox {
    private double minLat;
    private double maxLat;
    private double minLon;
    private double maxLon;
}
