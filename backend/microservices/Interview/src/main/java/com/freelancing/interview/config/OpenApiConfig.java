package com.freelancing.interview.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI interviewOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Interview service API")
                        .description("Interview scheduling and metrics per candidature")
                        .version("1.0"));
    }
}
