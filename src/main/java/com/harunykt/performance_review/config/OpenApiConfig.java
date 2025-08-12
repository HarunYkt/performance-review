package com.harunykt.performance_review.config;

import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.OpenAPI;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    @Bean
    public OpenAPI apiInfo() {
        return new OpenAPI().info(
                new Info()
                        .title("Performance Review API")
                        .version("v1")
                        .description("180° değerlendirme servisi dokümantasyonu")
        );
    }
}
