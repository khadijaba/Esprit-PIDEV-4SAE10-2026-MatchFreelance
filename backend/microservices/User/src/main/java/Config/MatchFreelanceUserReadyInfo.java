package Config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * Message explicite en console : le demarrage Spring resemble toujours au meme ;
 * les "modifications" se verifient via le navigateur ou les URLs ci-dessous.
 */
@Component
public class MatchFreelanceUserReadyInfo implements ApplicationListener<ApplicationReadyEvent> {

    private static final Logger log = LoggerFactory.getLogger("MATCHFREELANCE-USER");

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        Environment env = event.getApplicationContext().getEnvironment();
        String port = env.getProperty("local.server.port");
        if (port == null || port.isBlank()) {
            port = env.getProperty("server.port", "8098");
        }
        String base = "http://localhost:" + port;

        log.info("");
        log.info("------------------------------------------------------------------");
        log.info("  MatchFreelance USER est PRET (port {})", port);
        log.info("  Tester l'API :");
        log.info("    GET  {}/api/users/welcome", base);
        log.info("    POST {}/api/users/auth/login   (JSON email + password)", base);
        log.info("    GET  {}/api/users              (liste comptes)", base);
        log.info("  Voir les utilisateurs dans l'interface :");
        log.info("    1) ng serve sur http://localhost:4200");
        log.info("    2) Connexion ADMIN puis http://localhost:4200/admin/dashboard");
        log.info("  SendGrid : les logs ci-dessus = cle chargee ; mail = apres signup/reset.");
        log.info("------------------------------------------------------------------");
        log.info("");
    }
}
