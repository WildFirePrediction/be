package com.capstone25.WildFirePrediction.repository;

import com.capstone25.WildFirePrediction.domain.Earthquake;
import java.util.Set;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface EarthquakeRepository extends JpaRepository<Earthquake, Long> {

    boolean existsByEarthquakeNo(String earthquakeNo);

    @Query("select e.earthquakeNo from Earthquake e where e.earthquakeNo in :nos")
    Set<String> findExistingNos(Set<String> nos);
}
