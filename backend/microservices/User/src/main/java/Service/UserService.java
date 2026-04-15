package Service;

import DTO.RegistrationResult;
import DTO.SignUpRequest;
import DTO.PasswordResetRequest;
import DTO.PasswordResetConfirm;
import DTO.PasswordResetCompleteRequest;
import DTO.EmailVerificationRequest;
import DTO.ResendVerificationRequest;
import DTO.PasswordChangeRequest;
import Entity.User;
import Entity.Role;
import Repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final SendGridEmailService emailService;
    private final boolean continueOnSendFailure;

    private final java.util.Map<String, String> resetTokens = new java.util.concurrent.ConcurrentHashMap<>();
    private final java.util.Map<String, LocalDateTime> tokenExpirations = new java.util.concurrent.ConcurrentHashMap<>();

    private final java.util.Map<String, String> emailVerificationCodes = new java.util.concurrent.ConcurrentHashMap<>();
    private final java.util.Map<String, LocalDateTime> emailCodeExpirations = new java.util.concurrent.ConcurrentHashMap<>();

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder,
            SendGridEmailService emailService,
            @Value("${matchfreelance.email.continue-on-send-failure:true}") boolean continueOnSendFailure) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.continueOnSendFailure = continueOnSendFailure;
    }

    private String generateVerificationCode() {
        return String.format("%06d", (int) (Math.random() * 1000000));
    }

    public RegistrationResult register(SignUpRequest request, MultipartFile multipartFile) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Cet email est déjà utilisé");
        }
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new RuntimeException("Les mots de passe ne correspondent pas");
        }

        String profilePictureUrl = null;
        MultipartFile file = multipartFile;
        if (file == null || file.isEmpty()) {
            file = request.getFile();
        }
        if (file != null && !file.isEmpty()) {
            try {
                Path uploadDir = Paths.get("uploads", "avatars");
                if (!Files.exists(uploadDir)) {
                    Files.createDirectories(uploadDir);
                }
                String originalFilename = StringUtils.cleanPath(file.getOriginalFilename());
                String extension = "";
                int dot = originalFilename.lastIndexOf('.');
                if (dot > 0) {
                    extension = originalFilename.substring(dot);
                }
                String newFilename = "signup_" + UUID.randomUUID() + extension;
                Path target = uploadDir.resolve(newFilename);
                Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
                profilePictureUrl = "/uploads/avatars/" + newFilename;
            } catch (IOException e) {
                throw new RuntimeException("Erreur lors de l'enregistrement de l'avatar : " + e.getMessage());
            }
        }

        User user = User.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .address(request.getAddress())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .birthDate(request.getBirthDate())
                .role(request.getRole())
                .faceDescriptor(request.getFaceDescriptor())
                .profilePictureUrl(profilePictureUrl)
                .enabled(false)
                .build();

        User savedUser = userRepository.save(user);

        String verificationCode = generateVerificationCode();
        emailVerificationCodes.put(savedUser.getEmail(), verificationCode);
        emailCodeExpirations.put(savedUser.getEmail(), LocalDateTime.now().plusHours(24));
        logger.info("Verification code for {} : {}", savedUser.getEmail(), verificationCode);

        try {
            emailService.sendWelcomeEmail(savedUser.getEmail(), savedUser.getFirstName(), savedUser.getLastName());
            emailService.sendEmailVerification(savedUser.getEmail(), savedUser.getFirstName(), verificationCode);
            logger.info("SendGrid emails sent for {}", savedUser.getEmail());
        } catch (Exception e) {
            logger.error("SendGrid failed for {} : {}", savedUser.getEmail(), e.getMessage(), e);
            if (!continueOnSendFailure) {
                throw new RuntimeException(
                        "Compte cree mais envoi email echoue. Verifiez sendgrid.api.key et l expediteur verifie. "
                                + e.getMessage());
            }
            logger.warn("continue-on-send-failure=true: account kept; check logs or verificationCode in API response.");
        }

        return new RegistrationResult(savedUser, verificationCode);
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public void deleteUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé avec l'id : " + id));

        if (user.getRole() == Role.ADMIN) {
            throw new RuntimeException("Impossible de supprimer l'administrateur");
        }

        emailService.sendAccountDeletionEmail(user.getEmail(), user.getFirstName(), "Suppression par l'administrateur");

        userRepository.deleteById(id);
    }

    public void requestPasswordReset(PasswordResetRequest request) {
        User user = userRepository.findByEmail(request.getEmail().trim())
                .orElseThrow(() -> new RuntimeException("Aucun utilisateur trouvé avec cet email"));

        String verificationCode = generateVerificationCode();
        emailVerificationCodes.put(user.getEmail(), verificationCode);
        emailCodeExpirations.put(user.getEmail(), LocalDateTime.now().plusHours(24));
        logger.info("Password reset code for {} : {}", user.getEmail(), verificationCode);

        try {
            emailService.sendPasswordResetEmailWithCode(user.getEmail(), user.getFirstName(), verificationCode);
        } catch (Exception e) {
            logger.error("SendGrid password reset failed for {} : {}", user.getEmail(), e.getMessage(), e);
            if (!continueOnSendFailure) {
                emailVerificationCodes.remove(user.getEmail());
                emailCodeExpirations.remove(user.getEmail());
                throw new RuntimeException(
                        "Envoi de l'e-mail échoué. Vérifiez SendGrid. " + e.getMessage());
            }
            logger.warn("continue-on-send-failure=true: code reset dans les logs pour {}", user.getEmail());
        }
    }

    public String completePasswordResetWithCode(PasswordResetCompleteRequest request) {
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new RuntimeException("Les mots de passe ne correspondent pas");
        }
        String email = request.getEmail().trim();
        String storedCode = emailVerificationCodes.get(email);
        if (storedCode == null) {
            throw new RuntimeException("Aucun code de réinitialisation pour cet e-mail — refaites une demande.");
        }
        if (!storedCode.equals(request.getVerificationCode().trim())) {
            throw new RuntimeException("Code incorrect");
        }
        LocalDateTime expiration = emailCodeExpirations.get(email);
        if (expiration == null || LocalDateTime.now().isAfter(expiration)) {
            emailVerificationCodes.remove(email);
            emailCodeExpirations.remove(email);
            throw new RuntimeException("Code expiré — demandez un nouvel e-mail.");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setEnabled(true);
        userRepository.save(user);

        emailVerificationCodes.remove(email);
        emailCodeExpirations.remove(email);

        try {
            emailService.sendPasswordResetConfirmationEmail(email);
        } catch (Exception e) {
            logger.warn("E-mail de confirmation après reset non envoyé : {}", e.getMessage());
        }

        return "Mot de passe réinitialisé. Vous pouvez vous connecter.";
    }

    public String confirmPasswordReset(PasswordResetConfirm request) {
        String email = resetTokens.get(request.getToken());
        if (email == null) {
            throw new RuntimeException("Token de réinitialisation invalide");
        }

        LocalDateTime expiration = tokenExpirations.get(request.getToken());
        if (expiration == null || LocalDateTime.now().isAfter(expiration)) {
            resetTokens.remove(request.getToken());
            tokenExpirations.remove(request.getToken());
            throw new RuntimeException("Token de réinitialisation expiré");
        }

        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new RuntimeException("Les mots de passe ne correspondent pas");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        resetTokens.remove(request.getToken());
        tokenExpirations.remove(request.getToken());

        emailService.sendPasswordResetConfirmationEmail(email);

        return "Mot de passe réinitialisé avec succès";
    }

    public String verifyEmail(EmailVerificationRequest request) {
        String storedCode = emailVerificationCodes.get(request.getEmail());
        if (storedCode == null) {
            throw new RuntimeException("Aucun code de vérification trouvé pour cet email");
        }

        if (!storedCode.equals(request.getVerificationCode())) {
            throw new RuntimeException("Code de vérification incorrect");
        }

        LocalDateTime exp = emailCodeExpirations.get(request.getEmail());
        if (exp == null || LocalDateTime.now().isAfter(exp)) {
            emailVerificationCodes.remove(request.getEmail());
            emailCodeExpirations.remove(request.getEmail());
            throw new RuntimeException("Code de vérification expiré");
        }

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        user.setEnabled(true);
        userRepository.save(user);

        emailVerificationCodes.remove(request.getEmail());
        emailCodeExpirations.remove(request.getEmail());

        return "Email vérifié avec succès. Votre compte est maintenant activé.";
    }

    public String resendVerificationEmail(ResendVerificationRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Aucun utilisateur trouvé avec cet email"));

        if (user.isEnabled()) {
            throw new RuntimeException("Ce compte est déjà vérifié");
        }

        String verificationCode = generateVerificationCode();
        emailVerificationCodes.put(user.getEmail(), verificationCode);
        emailCodeExpirations.put(user.getEmail(), LocalDateTime.now().plusHours(24));
        logger.info("Resend verification code for {} : {}", user.getEmail(), verificationCode);

        emailService.resendVerificationEmail(user.getEmail(), user.getFirstName(), verificationCode);

        return "Un nouvel email de vérification a été envoyé à " + user.getEmail();
    }

    public String changePasswordWithCode(PasswordChangeRequest request) {
        String storedCode = emailVerificationCodes.get(request.getEmail());
        if (storedCode == null) {
            throw new RuntimeException("Aucun code de vérification trouvé pour cet email");
        }

        if (!storedCode.equals(request.getVerificationCode())) {
            throw new RuntimeException("Code de vérification incorrect");
        }

        LocalDateTime expiration = emailCodeExpirations.get(request.getEmail());
        if (expiration == null || LocalDateTime.now().isAfter(expiration)) {
            emailVerificationCodes.remove(request.getEmail());
            emailCodeExpirations.remove(request.getEmail());
            throw new RuntimeException("Code de vérification expiré");
        }

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new RuntimeException("Ancien mot de passe incorrect");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        emailVerificationCodes.remove(request.getEmail());
        emailCodeExpirations.remove(request.getEmail());

        logger.info("Password changed successfully for user: {}", request.getEmail());

        return "Mot de passe changé avec succès";
    }

    public void updateUserStatus(Long userId, Boolean enabled) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        if (user.getRole() == Role.ADMIN && !enabled) {
            throw new RuntimeException("Impossible de désactiver un utilisateur administrateur");
        }

        user.setEnabled(enabled);
        userRepository.save(user);

        logger.info("User status updated: {} is now {}", user.getEmail(), enabled ? "enabled" : "disabled");
    }

    private double calculateEuclideanDistance(String desc1, String desc2) {
        if (desc1 == null || desc2 == null) {
            return 1.0;
        }
        String[] d1Str = desc1.split(",");
        String[] d2Str = desc2.split(",");
        if (d1Str.length != 128 || d2Str.length != 128) {
            return 1.0;
        }

        double sum = 0;
        for (int i = 0; i < 128; i++) {
            try {
                double diff = Double.parseDouble(d1Str[i]) - Double.parseDouble(d2Str[i]);
                sum += diff * diff;
            } catch (NumberFormatException e) {
                return 1.0;
            }
        }
        return Math.sqrt(sum);
    }

    public User signInWithFace(String email, String loginDescriptor) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadCredentialsException("Email ou visage incorrect"));

        if (!user.isEnabled()) {
            throw new DisabledException(
                    "Ce compte est désactivé. Veuillez vérifier votre email pour activer votre compte.");
        }

        if (user.getFaceDescriptor() == null || user.getFaceDescriptor().trim().isEmpty()) {
            throw new BadCredentialsException(
                    "Le login par reconnaissance faciale n'est pas configuré pour cet utilisateur.");
        }

        double distance = calculateEuclideanDistance(user.getFaceDescriptor(), loginDescriptor);
        if (distance > 0.6) {
            throw new BadCredentialsException("Visage non reconnu.");
        }

        return user;
    }
}
