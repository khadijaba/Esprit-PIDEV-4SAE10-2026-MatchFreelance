package Service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * DEPRECATED: use {@link LoggingEmailService} (journalisation uniquement).
 * 
 * This class is kept for reference only and is NOT registered as a service.
 * Les envois réels ne sont pas configurés dans ce dépôt.
 * 
 * Email Service for sending password reset emails
 * 
 * SETUP INSTRUCTIONS:
 * (Historique SendGrid retiré du code.)
 */
// @Service - DISABLED
public class EmailService {

    // @Autowired - DISABLED
    private JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    // Go to: Settings > Sender Authentication > Add and verify your email

    public void sendPasswordResetEmail(String toEmail, String resetToken) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(toEmail);
        message.setSubject("Réinitialisation de votre mot de passe");
        
        String resetLink = "http://localhost:8080/api/auth/reset-password/confirm?token=" + resetToken;
        
        message.setText("Bonjour,\n\n" +
                     "Vous avez demandé la réinitialisation de votre mot de passe. " +
                     "Cliquez sur le lien ci-dessous pour réinitialiser votre mot de passe:\n\n" +
                     resetLink + "\n\n" +
                     "Ce lien expirera dans 15 minutes.\n\n" +
                     "Si vous n'avez pas demandé cette réinitialisation, veuillez ignorer cet email.\n\n" +
                     "Cordialement,\n" +
                     "L'équipe de support");
        
        mailSender.send(message);
    }

    public void sendPasswordResetConfirmationEmail(String toEmail) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(toEmail);
        message.setSubject("Confirmation de réinitialisation du mot de passe");
        
        message.setText("Bonjour,\n\n" +
                     "Votre mot de passe a été réinitialisé avec succès.\n\n" +
                     "Si vous n'avez pas effectué cette action, veuillez contacter immédiatement notre support.\n\n" +
                     "Cordialement,\n" +
                     "L'équipe de support");
        
        mailSender.send(message);
    }

    // Email de bienvenue après inscription
    public void sendWelcomeEmail(String toEmail, String firstName, String lastName) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(toEmail);
        message.setSubject("Bienvenue sur notre plateforme !");
        
        message.setText("Cher " + firstName + " " + lastName + ",\n\n" +
                     "Nous sommes ravis de vous accueillir sur notre plateforme !\n\n" +
                     "Votre compte a été créé avec succès.\n" +
                     "Vous pouvez maintenant vous connecter et commencer à utiliser nos services.\n\n" +
                     "Si vous avez des questions, n'hésitez pas à nous contacter.\n\n" +
                     "Cordialement,\n" +
                     "L'équipe de support");
        
        mailSender.send(message);
    }

    // Email de vérification de compte
    public void sendEmailVerification(String toEmail, String firstName, String verificationToken) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(toEmail);
        message.setSubject("Vérification de votre adresse email");
        
        String verificationLink = "http://localhost:8080/api/auth/verify-email?token=" + verificationToken + "&email=" + toEmail;
        
        message.setText("Cher " + firstName + ",\n\n" +
                     "Merci de vous être inscrit sur notre plateforme !\n\n" +
                     "Pour activer votre compte, veuillez vérifier votre adresse email en cliquant sur le lien ci-dessous:\n\n" +
                     verificationLink + "\n\n" +
                     "Ce lien expirera dans 24 heures.\n\n" +
                     "Si vous n'avez pas créé de compte, veuillez ignorer cet email.\n\n" +
                     "Cordialement,\n" +
                     "L'équipe de support");
        
        mailSender.send(message);
    }

    // Email de notification de suppression de compte
    public void sendAccountDeletionEmail(String toEmail, String firstName, String reason) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(toEmail);
        message.setSubject("Notification de suppression de compte");
        
        message.setText("Cher " + firstName + ",\n\n" +
                     "Nous vous informons que votre compte a été supprimé de notre plateforme.\n\n" +
                     "Raison de la suppression: " + reason + "\n\n" +
                     "Toutes vos données personnelles ont été supprimées conformément à notre politique de confidentialité.\n\n" +
                     "Si vous pensez qu'il s'agit d'une erreur ou si vous souhaitez réactiver votre compte,\n" +
                     "veuillez nous contacter dans les plus brefs délais.\n\n" +
                     "Nous vous remercions d'avoir utilisé notre plateforme.\n\n" +
                     "Cordialement,\n" +
                     "L'équipe de support");
        
        mailSender.send(message);
    }

    // Email de renvoi de vérification
    public void resendVerificationEmail(String toEmail, String firstName, String verificationToken) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(toEmail);
        message.setSubject("Renvoi de l'email de vérification");
        
        String verificationLink = "http://localhost:8080/api/auth/verify-email?token=" + verificationToken + "&email=" + toEmail;
        
        message.setText("Cher " + firstName + ",\n\n" +
                     "Vous avez demandé un nouvel email de vérification.\n\n" +
                     "Veuillez cliquer sur le lien ci-dessous pour vérifier votre adresse email:\n\n" +
                     verificationLink + "\n\n" +
                     "Ce lien expirera dans 24 heures.\n\n" +
                     "Si vous n'avez pas demandé cet email, veuillez ignorer cet email.\n\n" +
                     "Cordialement,\n" +
                     "L'équipe de support");
        
        mailSender.send(message);
    }
}
