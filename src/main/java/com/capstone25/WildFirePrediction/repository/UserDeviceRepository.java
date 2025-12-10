package com.capstone25.WildFirePrediction.repository;

import com.capstone25.WildFirePrediction.domain.UserDevice;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserDeviceRepository extends JpaRepository<UserDevice, Long> {
    Optional<UserDevice> findByDeviceUuid(String deviceUuid);
}
