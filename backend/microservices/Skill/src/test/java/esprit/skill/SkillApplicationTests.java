package esprit.skill;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Profil {@code test} : H2 + Eureka désactivés pour {@code mvn install} sans MySQL sur localhost.
 * Exécution réelle : MySQL + Eureka ({@code application.properties}).
 */
@SpringBootTest
@ActiveProfiles("test")
class SkillApplicationTests {

    @Test
    void contextLoads() {
    }
}
