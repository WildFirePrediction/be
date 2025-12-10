package com.capstone25.WildFirePrediction.service;

import com.capstone25.WildFirePrediction.domain.Region;
import com.capstone25.WildFirePrediction.domain.UserDevice;
import com.capstone25.WildFirePrediction.domain.UserRegionPreference;
import com.capstone25.WildFirePrediction.repository.RegionRepository;
import com.capstone25.WildFirePrediction.repository.UserDeviceRepository;
import com.capstone25.WildFirePrediction.repository.UserRegionPreferenceRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserPreferenceService {

    private final UserDeviceRepository userDeviceRepository;
    private final RegionRepository regionRepository;
    private final UserRegionPreferenceRepository preferenceRepository;

    // 선호지역 설정 (최대 3개, 중복 불가)
    @Transactional
    public void setPreferences(String deviceUuid, List<Long> regionIds) {
        if (regionIds.size() > 3) {
            throw new IllegalArgumentException("선호지역은 최대 3개까지 설정할 수 있습니다.");
        }

        // 1. UserDevice 찾거나 신규 생성
        UserDevice userDevice = userDeviceRepository.findByDeviceUuid(deviceUuid)
                .orElseGet(() -> userDeviceRepository.save(
                        UserDevice.builder().deviceUuid(deviceUuid).build()
                ));

        // 2. 기존 preferences 삭제
        preferenceRepository.deleteByUserDevice(userDevice);

        // 3. 새로운 preferences 저장
        for (Long regionId : regionIds) {
            Region region = regionRepository.findById(regionId)
                    .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 지역입니다."));

            UserRegionPreference pref = UserRegionPreference.builder()
                    .userDevice(userDevice)
                    .region(region)
                    .build();

            preferenceRepository.save(pref);
        }
    }

    // 선호지역 조회
    @Transactional(readOnly = true)
    public List<UserRegionPreference> getPreferences(String deviceUuid) {
        UserDevice userDevice = userDeviceRepository.findByDeviceUuid(deviceUuid)
                .orElseThrow(() -> new IllegalArgumentException("등록되지 않은 기기입니다."));
        return preferenceRepository.findByUserDevice(userDevice);
    }
}
