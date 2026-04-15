package com.freelancing.contract.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI contractOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Contract service API")
                        .description("Contracts, messages, financial endpoints")
                        .version("1.0"));
    }
}
