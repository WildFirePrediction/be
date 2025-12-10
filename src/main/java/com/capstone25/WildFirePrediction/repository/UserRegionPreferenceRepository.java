package com.capstone25.WildFirePrediction.repository;

import com.capstone25.WildFirePrediction.domain.UserDevice;
import com.capstone25.WildFirePrediction.domain.UserRegionPreference;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRegionPreferenceRepository extends JpaRepository<UserRegionPreference, Long> {

    List<UserRegionPreference> findByUserDevice(UserDevice userDevice);

    void deleteByUserDevice(UserDevice userDevice);

    long countByUserDevice(UserDevice userDevice);
}
