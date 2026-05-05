package Service;

import DTO.SignUpRequest;
import DTO.PasswordResetRequest;
import DTO.PasswordResetConfirm;
import DTO.EmailVerificationRequest;
import DTO.ResendVerificationRequest;
import DTO.PasswordChangeRequest;
import Entity.User;
import Entity.Role;
import Repository.UserRepository;
import Service.SendGridEmailService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;

import java.util.List;
import java.util.UUID;
import java.time.LocalDateTime;

@Service
public class UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final SendGridEmailService emailService;

    // Store reset tokens with expiration (in a real app, use a proper cache or
    // database)
    private final java.util.Map<String, String> resetTokens = new java.util.concurrent.ConcurrentHashMap<>();
    private final java.util.Map<String, LocalDateTime> tokenExpirations = new java.util.concurrent.ConcurrentHashMap<>();

    // Store email verification codes (6-digit codes)
    private final java.util.Map<String, String> emailVerificationCodes = new java.util.concurrent.ConcurrentHashMap<>();
    private final java.util.Map<String, LocalDateTime> emailCodeExpirations = new java.util.concurrent.ConcurrentHashMap<>();

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder,
            SendGridEmailService emailService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
    }

    // Generate 6-digit verification code
    private String generateVerificationCode() {
        return String.format("%06d", (int) (Math.random() * 1000000));
    }

    public User register(SignUpRequest request, org.springframework.web.multipart.MultipartFile file) {

        // Vérification email déjà utilisé
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Cet email est déjà utilisé");
        }

        // Vérification confirmation mot de passe
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new RuntimeException("Les mots de passe ne correspondent pas");
        }

        // Empêcher la création d'un second ADMIN via signup
        if (request.getRole() == Role.ADMIN) {
            throw new RuntimeException("Impossible de créer un compte admin via l'inscription");
        }

        String profilePictureUrl = null;
        if (file != null && !file.isEmpty()) {
            try {
                java.nio.file.Path uploadDir = java.nio.file.Paths.get("uploads", "avatars");
                if (!java.nio.file.Files.exists(uploadDir)) {
                    java.nio.file.Files.createDirectories(uploadDir);
                }
                String originalFilename = org.springframework.util.StringUtils.cleanPath(file.getOriginalFilename());
                String extension = "";
                int i = originalFilename.lastIndexOf('.');
                if (i > 0) {
                    extension = originalFilename.substring(i);
                }
                String newFilename = "user_reg_" + UUID.randomUUID().toString() + extension;
                java.nio.file.Path targetLocation = uploadDir.resolve(newFilename);
                java.nio.file.Files.copy(file.getInputStream(), targetLocation,
                        java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                profilePictureUrl = "/uploads/avatars/" + newFilename;
            } catch (java.io.IOException ex) {
                // Ignore failure to strictly bind the registration flow with minor file
                // processing errors
                logger.error("Failed to upload avatar during registration", ex);
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
                .enabled(false) // Account disabled until email verification
                .build();

        User savedUser = userRepository.save(user);

        try {
            String verificationCode = generateVerificationCode();
            emailVerificationCodes.put(savedUser.getEmail(), verificationCode);
            emailCodeExpirations.put(savedUser.getEmail(), LocalDateTime.now().plusHours(24));

            logger.info("📧 ENVOI D'EMAILS DÉSACTIVÉ POUR TEST - Code généré pour: {}", savedUser.getEmail());
            logger.info("🔑 DEBUG CODE (offline testing): {} for email: {}", verificationCode, savedUser.getEmail());
            emailService.sendWelcomeEmail(savedUser.getEmail(), savedUser.getFirstName(), savedUser.getLastName());
            emailService.sendEmailVerification(savedUser.getEmail(), savedUser.getFirstName(), verificationCode);
            logger.info("✓ Simulation d'envoi d'emails terminée");
        } catch (Exception e) {
            logger.error("✗ ERREUR lors de l'envoi des emails: {}", e.getMessage(), e);
            throw new RuntimeException(
                    "Compte créé mais erreur lors de l'envoi de l'email de vérification: " + e.getMessage());
        }

        return savedUser;
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

    // Password reset methods
    public void requestPasswordReset(PasswordResetRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Aucun utilisateur trouvé avec cet email"));

        // Generate 6-digit verification code
        String verificationCode = generateVerificationCode();
        emailVerificationCodes.put(user.getEmail(), verificationCode);
        emailCodeExpirations.put(user.getEmail(), LocalDateTime.now().plusHours(24));

        logger.info("📧 ENVOI D'EMAIL DE RESET DÉSACTIVÉ POUR TEST - Code: {}", verificationCode);
        logger.info("🔑 DEBUG RESET CODE (offline testing): {} for email: {}", verificationCode, user.getEmail());

        // Send email with verification code
        emailService.sendPasswordResetEmailWithCode(user.getEmail(), user.getFirstName(), verificationCode);
    }

    public String confirmPasswordReset(PasswordResetConfirm request) {
        // Validate token
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

        // Validate password confirmation
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new RuntimeException("Les mots de passe ne correspondent pas");
        }

        // Find user and update password
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        // Clean up token
        resetTokens.remove(request.getToken());
        tokenExpirations.remove(request.getToken());

        emailService.sendPasswordResetConfirmationEmail(email);

        return "Mot de passe réinitialisé avec succès";
    }

    // Email verification methods
    public String verifyEmail(EmailVerificationRequest request) {
        // Validate code
        String storedCode = emailVerificationCodes.get(request.getEmail());
        if (storedCode == null) {
            throw new RuntimeException("Aucun code de vérification trouvé pour cet email");
        }

        // Check if code matches
        if (!storedCode.equals(request.getVerificationCode())) {
            throw new RuntimeException("Code de vérification incorrect");
        }

        LocalDateTime expiration = emailCodeExpirations.get(request.getEmail());
        if (expiration == null || LocalDateTime.now().isAfter(expiration)) {
            emailVerificationCodes.remove(request.getEmail());
            emailCodeExpirations.remove(request.getEmail());
            throw new RuntimeException("Code de vérification expiré");
        }

        // Find user and enable account
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        user.setEnabled(true);
        userRepository.save(user);

        // Clean up code
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

        // Generate new verification code
        String verificationCode = generateVerificationCode();
        emailVerificationCodes.put(user.getEmail(), verificationCode);
        emailCodeExpirations.put(user.getEmail(), LocalDateTime.now().plusHours(24));

        logger.info("📧 ENVOI D'EMAIL DE RENVOI DÉSACTIVÉ POUR TEST - Code: {}", verificationCode);
        logger.info("🔑 DEBUG RESEND CODE (offline testing): {} for email: {}", verificationCode, user.getEmail());

        emailService.resendVerificationEmail(user.getEmail(), user.getFirstName(), verificationCode);

        return "Un nouvel email de vérification a été envoyé à " + user.getEmail();
    }

    public String changePasswordWithCode(PasswordChangeRequest request) {
        // Validate verification code
        String storedCode = emailVerificationCodes.get(request.getEmail());
        if (storedCode == null) {
            throw new RuntimeException("Aucun code de vérification trouvé pour cet email");
        }

        // Check if code matches
        if (!storedCode.equals(request.getVerificationCode())) {
            throw new RuntimeException("Code de vérification incorrect");
        }

        LocalDateTime expiration = emailCodeExpirations.get(request.getEmail());
        if (expiration == null || LocalDateTime.now().isAfter(expiration)) {
            emailVerificationCodes.remove(request.getEmail());
            emailCodeExpirations.remove(request.getEmail());
            throw new RuntimeException("Code de vérification expiré");
        }

        // Find user
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        // Verify old password
        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new RuntimeException("Ancien mot de passe incorrect");
        }

        // Update password
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        // Clean up verification code
        emailVerificationCodes.remove(request.getEmail());
        emailCodeExpirations.remove(request.getEmail());

        logger.info("✓ Password changed successfully for user with ID: {}", user.getId());

        return "Mot de passe changé avec succès";
    }

    public void updateUserStatus(Long userId, Boolean enabled) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        // Prevent disabling admin users
        if (user.getRole() == Role.ADMIN && !enabled) {
            throw new RuntimeException("Impossible de désactiver un utilisateur administrateur");
        }

        user.setEnabled(enabled);
        userRepository.save(user);

        logger.info("✓ User status updated: {} is now {}", user.getEmail(), enabled ? "enabled" : "disabled");
    }

    private double calculateEuclideanDistance(String desc1, String desc2) {
        if (desc1 == null || desc2 == null)
            return 1.0;
        String[] d1Str = desc1.split(",");
        String[] d2Str = desc2.split(",");
        if (d1Str.length != 128 || d2Str.length != 128)
            return 1.0;

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
