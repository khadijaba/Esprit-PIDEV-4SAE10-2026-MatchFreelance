package esprit.skill.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI skillOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Skill service API")
                        .description("Skills, CV, portfolio")
                        .version("1.0"));
    }
}
