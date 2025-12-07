package com.capstone25.WildFirePrediction;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@EnableJpaAuditing
@SpringBootApplication
public class WildFirePredictionApplication {

	public static void main(String[] args) {
		SpringApplication.run(WildFirePredictionApplication.class, args);
	}

}
