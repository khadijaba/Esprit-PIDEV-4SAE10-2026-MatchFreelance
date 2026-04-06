package tn.esprit.formation.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI formationOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Formation service API")
                        .description("Formations, exams, inscriptions")
                        .version("1.0"));
    }
}
