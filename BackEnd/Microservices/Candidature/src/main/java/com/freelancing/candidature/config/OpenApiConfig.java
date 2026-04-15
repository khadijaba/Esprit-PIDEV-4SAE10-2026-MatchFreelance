package com.freelancing.candidature.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI candidatureOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Candidature service API")
                        .description("Applications, ranking, contract actions on candidature routes")
                        .version("1.0"));
    }
}
