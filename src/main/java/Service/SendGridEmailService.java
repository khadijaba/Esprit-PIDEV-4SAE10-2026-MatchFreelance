package Service;

import com.sendgrid.Method;
import com.sendgrid.Request;
import com.sendgrid.Response;
import com.sendgrid.SendGrid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;

/**
 * Production-ready SendGrid Email Service using official SendGrid Java SDK
 * 
 * SETUP INSTRUCTIONS:
 * 1. Create SendGrid account: https://signup.sendgrid.com/
 * 2. Generate API Key: Settings > API Keys > Create API Key
 * 3. Verify sender email: Settings > Sender Authentication > Single Sender Verification
 * 4. Set environment variable: SENDGRID_API_KEY=your_api_key
 * 5. Or update application.properties: sendgrid.api.key=your_api_key
 * 
 * WHY WE USE SendGrid SDK INSTEAD OF SMTP:
 * - Better error messages (we can see actual failure reasons)
 * - More reliable delivery
 * - Easier debugging with response status codes
 * - Proper JSON formatting for SendGrid API
 */
@Service
public class SendGridEmailService {

    private static final Logger logger = LoggerFactory.getLogger(SendGridEmailService.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final SendGrid sendGrid;

    @Value("${sendgrid.from.email:noreply@example.com}")
    private String fromEmail;

    @Value("${sendgrid.from.name:Support Team}")
    private String fromName;

    public SendGridEmailService(SendGrid sendGrid) {
        this.sendGrid = sendGrid;
    }

    /**
     * Send password reset email
     */
    public void sendPasswordResetEmail(String toEmail, String resetToken) {
        try {
            String resetLink = "http://localhost:3000/reset-password?token=" + resetToken;
            
            String htmlContent = """
                    <html>
                    <body style="font-family: Arial, sans-serif; line-height: 1.6; color: #333;">
                        <div style="max-width: 600px; margin: 0 auto; padding: 20px;">
                            <h2>Réinitialisation de votre mot de passe</h2>
                            <p>Vous avez demandé la réinitialisation de votre mot de passe.</p>
                            <p>Cliquez sur le bouton ci-dessous pour réinitialiser votre mot de passe:</p>
                            <p>
                                <a href="%s" style="background-color: #007bff; color: white; padding: 10px 20px; text-decoration: none; border-radius: 5px; display: inline-block;">
                                    Réinitialiser le mot de passe
                                </a>
                            </p>
                            <p style="color: #999; font-size: 12px;">Ce lien expirera dans 15 minutes.</p>
                            <p style="color: #999; font-size: 12px;">Si vous n'avez pas demandé cette réinitialisation, veuillez ignorer cet email.</p>
                        </div>
                    </body>
                    </html>
                    """.formatted(resetLink);

            sendMailViaSendGrid(toEmail, "Réinitialisation de votre mot de passe", htmlContent);
            logger.info("✓ Email de réinitialisation envoyé à: {}", toEmail);

        } catch (IOException e) {
            logger.error("✗ ERREUR lors de l'envoi de l'email de réinitialisation à {}: {}", toEmail, e.getMessage(), e);
            throw new RuntimeException("Échec de l'envoi de l'email", e);
        }
    }

    /**
     * Send password reset confirmation email
     */
    public void sendPasswordResetConfirmationEmail(String toEmail) {
        try {
            String htmlContent = """
                    <html>
                    <body style="font-family: Arial, sans-serif; line-height: 1.6; color: #333;">
                        <div style="max-width: 600px; margin: 0 auto; padding: 20px;">
                            <h2>Confirmation de réinitialisation du mot de passe</h2>
                            <p>Votre mot de passe a été réinitialisé avec succès.</p>
                            <p>Vous pouvez maintenant vous connecter avec votre nouveau mot de passe.</p>
                            <p style="color: #999; font-size: 12px;">Si vous n'avez pas effectué cette action, veuillez contacter immédiatement notre support.</p>
                        </div>
                    </body>
                    </html>
                    """;

            sendMailViaSendGrid(toEmail, "Confirmation de réinitialisation du mot de passe", htmlContent);
            logger.info("✓ Email de confirmation de réinitialisation envoyé à: {}", toEmail);

        } catch (IOException e) {
            logger.error("✗ ERREUR lors de l'envoi de l'email de confirmation à {}: {}", toEmail, e.getMessage(), e);
            throw new RuntimeException("Échec de l'envoi de l'email", e);
        }
    }

    /**
     * Send welcome email
     */
    public void sendWelcomeEmail(String toEmail, String firstName, String lastName) {
        try {
            String htmlContent = """
                    <html>
                    <body style="font-family: Arial, sans-serif; line-height: 1.6; color: #333;">
                        <div style="max-width: 600px; margin: 0 auto; padding: 20px;">
                            <h2>Bienvenue, %s %s !</h2>
                            <p>Nous sommes ravis de vous accueillir sur notre plateforme !</p>
                            <p>Votre compte a été créé avec succès.</p>
                            <p>Vous recevrez bientôt un email de vérification. Veuillez vérifier votre adresse email pour activer votre compte.</p>
                            <p style="color: #999; font-size: 12px;">Si vous avez des questions, n'hésitez pas à nous contacter.</p>
                        </div>
                    </body>
                    </html>
                    """.formatted(firstName, lastName);

            sendMailViaSendGrid(toEmail, "Bienvenue sur notre plateforme !", htmlContent);
            logger.info("✓ Email de bienvenue envoyé à: {}", toEmail);

        } catch (IOException e) {
            logger.error("✗ ERREUR lors de l'envoi de l'email de bienvenue à {}: {}", toEmail, e.getMessage(), e);
            throw new RuntimeException("Échec de l'envoi de l'email", e);
        }
    }

    /**
     * Send email verification with code
     */
    public void sendEmailVerification(String toEmail, String firstName, String verificationCode) {
        try {
            String htmlContent = """
                    <html>
                    <body style="font-family: Arial, sans-serif; line-height: 1.6; color: #333;">
                        <div style="max-width: 600px; margin: 0 auto; padding: 20px;">
                            <h2>Vérification de votre adresse email</h2>
                            <p>Cher %s,</p>
                            <p>Merci de vous être inscrit sur notre plateforme !</p>
                            <p>Pour activer votre compte, veuillez utiliser le code de vérification ci-dessous:</p>
                            <div style="background-color: #f8f9fa; border: 2px dashed #28a745; padding: 20px; margin: 20px 0; text-align: center;">
                                <h3 style="color: #28a745; font-size: 24px; letter-spacing: 3px; margin: 0;">%s</h3>
                            </div>
                            <p><strong>Instructions:</strong></p>
                            <ol>
                                <li>Allez sur la page de vérification</li>
                                <li>Entrez votre adresse email</li>
                                <li>Saisissez ce code de vérification: <strong>%s</strong></li>
                                <li>Cliquez sur "Vérifier Email"</li>
                            </ol>
                            <p style="color: #999; font-size: 12px;">Ce code expirera dans 24 heures.</p>
                            <p style="color: #999; font-size: 12px;">Si vous n'avez pas créé de compte, veuillez ignorer cet email.</p>
                        </div>
                    </body>
                    </html>
                    """.formatted(firstName, verificationCode, verificationCode);

            sendMailViaSendGrid(toEmail, "Vérification de votre adresse email", htmlContent);
            logger.info("✓ Email de vérification avec code envoyé à: {}", toEmail);

        } catch (IOException e) {
            logger.error("✗ ERREUR lors de l'envoi de l'email de vérification à {}: {}", toEmail, e.getMessage(), e);
            throw new RuntimeException("Échec de l'envoi de l'email", e);
        }
    }

    /**
     * Send account deletion notification
     */
    public void sendAccountDeletionEmail(String toEmail, String firstName, String reason) {
        try {
            String htmlContent = """
                    <html>
                    <body style="font-family: Arial, sans-serif; line-height: 1.6; color: #333;">
                        <div style="max-width: 600px; margin: 0 auto; padding: 20px;">
                            <h2>Notification de suppression de compte</h2>
                            <p>Cher %s,</p>
                            <p>Nous vous informons que votre compte a été supprimé de notre plateforme.</p>
                            <p><strong>Raison:</strong> %s</p>
                            <p>Toutes vos données personnelles ont été supprimées conformément à notre politique de confidentialité.</p>
                            <p>Si vous pensez qu'il s'agit d'une erreur ou si vous souhaitez réactiver votre compte, veuillez nous contacter dans les plus brefs délais.</p>
                            <p>Merci d'avoir utilisé notre plateforme.</p>
                        </div>
                    </body>
                    </html>
                    """.formatted(firstName, reason);

            sendMailViaSendGrid(toEmail, "Notification de suppression de compte", htmlContent);
            logger.info("✓ Email de suppression de compte envoyé à: {}", toEmail);

        } catch (IOException e) {
            logger.error("✗ ERREUR lors de l'envoi de l'email de suppression à {}: {}", toEmail, e.getMessage(), e);
            throw new RuntimeException("Échec de l'envoi de l'email", e);
        }
    }

    /**
     * Resend verification email with code
     */
    public void resendVerificationEmail(String toEmail, String firstName, String verificationCode) {
        try {
            String htmlContent = """
                    <html>
                    <body style="font-family: Arial, sans-serif; line-height: 1.6; color: #333;">
                        <div style="max-width: 600px; margin: 0 auto; padding: 20px;">
                            <h2>Renvoi de l'email de vérification</h2>
                            <p>Cher %s,</p>
                            <p>Vous avez demandé un nouvel email de vérification.</p>
                            <p>Veuillez utiliser le code de vérification ci-dessous:</p>
                            <div style="background-color: #f8f9fa; border: 2px dashed #28a745; padding: 20px; margin: 20px 0; text-align: center;">
                                <h3 style="color: #28a745; font-size: 24px; letter-spacing: 3px; margin: 0;">%s</h3>
                            </div>
                            <p><strong>Instructions:</strong></p>
                            <ol>
                                <li>Allez sur la page de vérification</li>
                                <li>Entrez votre adresse email</li>
                                <li>Saisissez ce code de vérification: <strong>%s</strong></li>
                                <li>Cliquez sur "Vérifier Email"</li>
                            </ol>
                            <p style="color: #999; font-size: 12px;">Ce code expirera dans 24 heures.</p>
                            <p style="color: #999; font-size: 12px;">Si vous n'avez pas demandé cet email, veuillez ignorer cet email.</p>
                        </div>
                    </body>
                    </html>
                    """.formatted(firstName, verificationCode, verificationCode);

            sendMailViaSendGrid(toEmail, "Renvoi de l'email de vérification", htmlContent);
            logger.info("✓ Email de renvoi de vérification avec code envoyé à: {}", toEmail);

        } catch (IOException e) {
            logger.error("✗ ERREUR lors de l'envoi de l'email de renvoi de vérification à {}: {}", toEmail, e.getMessage(), e);
            throw new RuntimeException("Échec de l'envoi de l'email", e);
        }
    }

    /**
     * Core method to send mail via SendGrid API
     * Handles all error logging and response validation
     */
    private void sendMailViaSendGrid(String toEmail, String subject, String htmlContent) throws IOException {
        try {
            // Validate inputs
            if (toEmail == null || toEmail.isEmpty()) {
                throw new IllegalArgumentException("Email recipient cannot be empty");
            }
            if (fromEmail == null || fromEmail.isEmpty()) {
                throw new IllegalArgumentException("Sender email not configured (sendgrid.from.email)");
            }

            logger.info("📧 Préparation de l'email: De={} → À={}, Sujet={}", fromEmail, toEmail, subject);

            // Build email JSON manually
            String emailJson = buildEmailJson(fromEmail, fromName, toEmail, subject, htmlContent);
            logger.info("📄 Email JSON payload: {}", emailJson);

            // Send via SendGrid API
            Request request = new Request();
            request.setMethod(Method.POST);
            request.setEndpoint("mail/send");
            request.setBody(emailJson);

            // Log request details
            logger.info("🚀 Envoi de la requête à SendGrid:");
            logger.info("   - Method: {}", request.getMethod());
            logger.info("   - Endpoint: {}", request.getEndpoint());
            logger.info("   - Body Length: {} characters", request.getBody().length());
            logger.info("   - From Email: {}", fromEmail);
            logger.info("   - To Email: {}", toEmail);

            Response response = sendGrid.api(request);

            // Log response details
            logger.info("📨 Réponse SendGrid reçue:");
            logger.info("   - Status Code: {}", response.getStatusCode());
            logger.info("   - Headers: {}", response.getHeaders());
            logger.info("   - Response Body: {}", response.getBody());
            
            if (response.getStatusCode() >= 200 && response.getStatusCode() < 300) {
                logger.info("✅ Email envoyé avec succès à {}", toEmail);
            } else {
                // Parse error response for better debugging
                String errorMessage = parseSendGridError(response.getBody());
                logger.error("❌ Erreur SendGrid détaillée:");
                logger.error("   - Status: {}", response.getStatusCode());
                logger.error("   - Body: {}", response.getBody());
                logger.error("   - Parsed Error: {}", errorMessage);
                logger.error("   - From Email Config: {}", fromEmail);
                logger.error("   - To Email: {}", toEmail);
                
                // Additional debugging for 403 errors
                if (response.getStatusCode() == 403) {
                    logger.error("🔍 403 Error - Vérifications à faire:");
                    logger.error("   1. L'email '{}' est-il bien vérifié dans SendGrid?", fromEmail);
                    logger.error("   2. La clé API a-t-elle les permissions 'Mail Send'?");
                    logger.error("   3. Le compte SendGrid est-il actif et non en sandbox?");
                }
                
                throw new RuntimeException("SendGrid API error: " + response.getStatusCode() + " - " + errorMessage);
            }

        } catch (Exception e) {
            logger.error("💥 Exception lors de l'envoi de l'email à {}: {} - {}", toEmail, e.getClass().getSimpleName(), e.getMessage(), e);
            throw new IOException("Failed to send email via SendGrid", e);
        }
    }

    /**
     * Test SendGrid API key and account status
     * Call this method to verify your SendGrid setup
     */
    public String testSendGridConfiguration() {
        try {
            logger.info("🔍 Test de la configuration SendGrid...");
            
            // Test API key by making a simple API call
            Request request = new Request();
            request.setMethod(Method.GET);
            request.setEndpoint("user/account");
            
            Response response = sendGrid.api(request);
            
            logger.info("📨 Réponse du test API:");
            logger.info("   - Status Code: {}", response.getStatusCode());
            logger.info("   - Response Body: {}", response.getBody());
            
            if (response.getStatusCode() == 200) {
                // Parse account info
                JsonNode accountInfo = objectMapper.readTree(response.getBody());
                String email = accountInfo.has("email") ? accountInfo.get("email").asText() : "N/A";
                String username = accountInfo.has("username") ? accountInfo.get("username").asText() : "N/A";
                
                logger.info("✅ Configuration SendGrid valide:");
                logger.info("   - Email du compte: {}", email);
                logger.info("   - Username: {}", username);
                
                // Now test sender verification
                return testSenderVerification();
                
            } else {
                String error = parseSendGridError(response.getBody());
                logger.error("❌ Erreur de configuration SendGrid: {}", error);
                return "❌ Erreur API Key: " + error;
            }
            
        } catch (Exception e) {
            logger.error("💥 Exception lors du test SendGrid: {}", e.getMessage(), e);
            return "❌ Exception: " + e.getMessage();
        }
    }
    
    /**
     * Test sender verification status
     */
    private String testSenderVerification() {
        try {
            logger.info("🔍 Test de la vérification des senders...");
            
            Request request = new Request();
            request.setMethod(Method.GET);
            request.setEndpoint("senders");
            
            Response response = sendGrid.api(request);
            
            logger.info("📨 Réponse des senders:");
            logger.info("   - Status Code: {}", response.getStatusCode());
            logger.info("   - Response Body: {}", response.getBody());
            
            if (response.getStatusCode() == 200) {
                JsonNode responseJson = objectMapper.readTree(response.getBody());
                
                // Check if response is an array
                if (responseJson.isArray()) {
                    JsonNode senders = responseJson;
                    boolean foundVerifiedSender = false;
                    
                    for (JsonNode sender : senders) {
                        if (sender.has("email") && sender.has("verified")) {
                            String senderEmail = sender.get("email").asText();
                            boolean verified = sender.get("verified").asBoolean();
                            
                            logger.info("📧 Sender trouvé: {} - Verified: {}", senderEmail, verified);
                            
                            if (senderEmail.equals(fromEmail) && verified) {
                                foundVerifiedSender = true;
                                logger.info("✅ Sender '{}' est bien vérifié!", fromEmail);
                            }
                        }
                    }
                    
                    if (!foundVerifiedSender) {
                        logger.error("❌ Sender '{}' non trouvé ou non vérifié dans la liste des senders", fromEmail);
                        return "❌ Sender '" + fromEmail + "' non vérifié. Vérifiez votre configuration SendGrid.";
                    }
                } else {
                    logger.error("❌ Réponse inattendue de l'API senders (pas un tableau)");
                    return "❌ Format de réponse inattendu";
                }
                
                return "✅ Configuration SendGrid OK - Sender vérifié";
                
            } else {
                String error = parseSendGridError(response.getBody());
                logger.error("❌ Erreur lors de la vérification des senders: {}", error);
                return "❌ Erreur senders: " + error;
            }
            
        } catch (Exception e) {
            logger.error("💥 Exception lors du test des senders: {}", e.getMessage(), e);
            return "❌ Exception senders: " + e.getMessage();
        }
    }
    private String parseSendGridError(String responseBody) {
        try {
            if (responseBody == null || responseBody.isEmpty()) {
                return "Unknown error - empty response body";
            }
            
            JsonNode jsonNode = objectMapper.readTree(responseBody);
            if (jsonNode.has("errors") && jsonNode.get("errors").isArray()) {
                JsonNode errors = jsonNode.get("errors");
                StringBuilder errorMessages = new StringBuilder();
                for (JsonNode error : errors) {
                    if (error.has("message")) {
                        if (errorMessages.length() > 0) {
                            errorMessages.append("; ");
                        }
                        errorMessages.append(error.get("message").asText());
                        
                        if (error.has("field") && !error.get("field").isNull()) {
                            errorMessages.append(" (field: ").append(error.get("field").asText()).append(")");
                        }
                    }
                }
                return errorMessages.length() > 0 ? errorMessages.toString() : responseBody;
            }
            return responseBody;
        } catch (Exception e) {
            logger.debug("Failed to parse SendGrid error response: {}", e.getMessage());
            return responseBody;
        }
    }

    /**
     * Build SendGrid email JSON payload
     */
    private String buildEmailJson(String fromEmail, String fromName, String toEmail, 
                                   String subject, String htmlContent) throws Exception {
        
        // Create a proper JSON structure using ObjectMapper for correct escaping
        var jsonNode = objectMapper.createObjectNode();
        
        // Add personalizations
        var personalization = objectMapper.createObjectNode();
        var toEmailArray = objectMapper.createArrayNode();
        var toEmailObj = objectMapper.createObjectNode();
        toEmailObj.put("email", toEmail);
        toEmailArray.add(toEmailObj);
        personalization.set("to", toEmailArray);
        
        var personalizationsArray = objectMapper.createArrayNode();
        personalizationsArray.add(personalization);
        jsonNode.set("personalizations", personalizationsArray);
        
        // Add from
        var fromObj = objectMapper.createObjectNode();
        fromObj.put("email", fromEmail);
        fromObj.put("name", fromName);
        jsonNode.set("from", fromObj);
        
        // Add subject
        jsonNode.put("subject", subject);
        
        // Add content
        var contentArray = objectMapper.createArrayNode();
        var contentObj = objectMapper.createObjectNode();
        contentObj.put("type", "text/html");
        contentObj.put("value", htmlContent);
        contentArray.add(contentObj);
        jsonNode.set("content", contentArray);
        
        return objectMapper.writeValueAsString(jsonNode);
    }

    /**
     * Send password reset email with verification code
     */
    public void sendPasswordResetEmailWithCode(String toEmail, String firstName, String verificationCode) {
        try {
            String subject = "Password Reset Verification Code";
            
            String htmlContent = 
                "<!DOCTYPE html>" +
                "<html>" +
                "<head>" +
                "    <meta charset='UTF-8'>" +
                "    <meta name='viewport' content='width=device-width, initial-scale=1.0'>" +
                "    <title>Password Reset Code</title>" +
                "    <style>" +
                "        body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; max-width: 600px; margin: 0 auto; padding: 20px; }" +
                "        .header { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; padding: 30px; text-align: center; border-radius: 10px 10px 0 0; }" +
                "        .content { background: #f9f9f9; padding: 30px; border-radius: 0 0 10px 10px; }" +
                "        .code-box { background: white; border: 2px dashed #667eea; padding: 20px; text-align: center; margin: 20px 0; border-radius: 8px; }" +
                "        .code { font-size: 32px; font-weight: bold; color: #667eea; letter-spacing: 5px; font-family: monospace; }" +
                "        .instructions { background: #e7f3ff; padding: 15px; border-radius: 5px; margin: 20px 0; }" +
                "        .footer { text-align: center; margin-top: 30px; color: #666; font-size: 12px; }" +
                "    </style>" +
                "</head>" +
                "<body>" +
                "    <div class='header'>" +
                "        <h1>🔐 Password Reset Verification</h1>" +
                "        <p>Hi " + firstName + ", we received a request to reset your password</p>" +
                "    </div>" +
                "    <div class='content'>" +
                "        <p>Use the verification code below to complete your password change:</p>" +
                "        <div class='code-box'>" +
                "            <div class='code'>" + verificationCode + "</div>" +
                "        </div>" +
                "        <div class='instructions'>" +
                "            <strong>Instructions:</strong><br>" +
                "            1. Return to the password change page<br>" +
                "            2. Enter this 6-digit code in the verification field<br>" +
                "            3. Complete your password change<br>" +
                "            <br>" +
                "            <strong>⏰ This code will expire in 24 hours</strong>" +
                "        </div>" +
                "        <p><strong>Security Notice:</strong> If you didn't request this password change, please ignore this email or contact support.</p>" +
                "    </div>" +
                "    <div class='footer'>" +
                "        <p>© 2024 FreelanceHub. All rights reserved.</p>" +
                "        <p>This is an automated message, please do not reply to this email.</p>" +
                "    </div>" +
                "</body>" +
                "</html>";

            sendMailViaSendGrid(toEmail, subject, htmlContent);
            logger.info("✓ Password reset email with code sent to: {}", toEmail);

        } catch (Exception e) {
            logger.error("✗ ERROR sending password reset email with code to {}: {}", toEmail, e.getMessage(), e);
            throw new RuntimeException("Failed to send password reset email", e);
        }
    }
}
