package com.capstone25.WildFirePrediction.config;

import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.OpenAPI;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI wfpAPI() {
        Info info = new Info()
                .title("Wild Fire Prediction BE API")
                .description("Wild Fire Prediction 백엔드 API 명세서")
                .version("1.0.0");

        return new OpenAPI()
                .info(info);
    }
}
