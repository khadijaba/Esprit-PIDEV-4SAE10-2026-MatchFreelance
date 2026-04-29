package Service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Remplace l’intégration SendGrid : aucun envoi HTTP, uniquement des traces pour le debug.
 */
@Service
public class LoggingEmailService {

    private static final Logger log = LoggerFactory.getLogger(LoggingEmailService.class);

    public boolean isDeliveryConfigured() {
        return false;
    }

    /** Ancien test SendGrid — conservé pour compatibilité d’URL éventuelle. */
    public String testSendGridConfiguration() {
        return "Envoi d’e-mails désactivé (logs serveur uniquement, pas de SendGrid).";
    }

    public void sendPasswordResetEmail(String toEmail, String resetToken) {
        log.info("[Email] reset lien (non envoyé) → {} token={}", toEmail, resetToken);
    }

    public void sendPasswordResetConfirmationEmail(String toEmail) {
        log.info("[Email] reset confirmé (non envoyé) → {}", toEmail);
    }

    public void sendWelcomeEmail(String toEmail, String firstName, String lastName) {
        log.info("[Email] bienvenue (non envoyé) → {} {} {}", firstName, lastName, toEmail);
    }

    public void sendEmailVerification(String toEmail, String firstName, String verificationCode) {
        log.info("[Email] vérif code (non envoyé) → {} {} code={}", firstName, toEmail, verificationCode);
    }

    public void sendAccountDeletionEmail(String toEmail, String firstName, String reason) {
        log.info("[Email] suppression compte (non envoyé) → {} {} raison={}", firstName, toEmail, reason);
    }

    public void resendVerificationEmail(String toEmail, String firstName, String verificationCode) {
        log.info("[Email] renvoi vérif (non envoyé) → {} {} code={}", firstName, toEmail, verificationCode);
    }

    public void sendPasswordResetEmailWithCode(String toEmail, String firstName, String verificationCode) {
        log.info("[Email] reset code (non envoyé) → {} {} code={}", firstName, toEmail, verificationCode);
    }
}
