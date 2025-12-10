package com.capstone25.WildFirePrediction.repository;

import com.capstone25.WildFirePrediction.domain.Region;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RegionRepository extends JpaRepository<Region, Long> {
    Optional<Region> findByAdminCode(String adminCode);
}
