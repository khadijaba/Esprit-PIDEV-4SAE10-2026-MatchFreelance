package esprit.project.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI projectOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Project service API")
                        .description("REST API for freelance projects")
                        .version("1.0"));
    }
}
