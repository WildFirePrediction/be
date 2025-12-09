package com.capstone25.WildFirePrediction.dto.projection;

public interface ShelterProjection {
    Long getId();
    String getFacilityName();
    String getRoadAddress();
    Double getLatitude();
    Double getLongitude();
    String getShelterTypeName();
    Double getDistanceKm();  // Haversine 계산 결과
}
