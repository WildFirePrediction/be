package com.capstone25.WildFirePrediction.dto.response;

import com.capstone25.WildFirePrediction.domain.UserRegionPreference;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class UserPreferenceResponse {
    private Long id;
    private RegionResponse region;

    public static UserPreferenceResponse from(UserRegionPreference preference) {
        return new UserPreferenceResponse(
                preference.getId(),
                new RegionResponse(
                        preference.getRegion().getId(),
                        preference.getRegion().getSido(),
                        preference.getRegion().getSigungu(),
                        preference.getRegion().getEupmyeondong()
                )
        );
    }
}
