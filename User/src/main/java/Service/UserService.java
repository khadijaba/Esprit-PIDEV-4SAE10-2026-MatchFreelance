package Service;


import DTO.SignUpRequest;
import DTO.PasswordResetRequest;
import DTO.PasswordResetConfirm;
import DTO.EmailVerificationRequest;
import DTO.ResendVerificationRequest;
import Entity.User;
import Entity.Role;
import Repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.time.LocalDateTime;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    // Store reset tokens with expiration (in a real app, use a proper cache or database)
    private final java.util.Map<String, String> resetTokens = new java.util.concurrent.ConcurrentHashMap<>();
    private final java.util.Map<String, LocalDateTime> tokenExpirations = new java.util.concurrent.ConcurrentHashMap<>();

    // Store email verification tokens
    private final java.util.Map<String, String> emailVerificationTokens = new java.util.concurrent.ConcurrentHashMap<>();
    private final java.util.Map<String, LocalDateTime> emailTokenExpirations = new java.util.concurrent.ConcurrentHashMap<>();

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder, EmailService emailService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
    }

    public User register(SignUpRequest request) {

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

        User user = User.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .address(request.getAddress())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .birthDate(request.getBirthDate())
                .role(request.getRole())
                .enabled(false) // Account disabled until email verification
                .build();

        User savedUser = userRepository.save(user);

        try {
            String verificationToken = UUID.randomUUID().toString();
            emailVerificationTokens.put(verificationToken, savedUser.getEmail());
            emailTokenExpirations.put(verificationToken, LocalDateTime.now().plusHours(24));

            emailService.sendWelcomeEmail(savedUser.getEmail(), savedUser.getFirstName(), savedUser.getLastName());
            emailService.sendEmailVerification(savedUser.getEmail(), savedUser.getFirstName(), verificationToken);
        } catch (Exception e) {
            System.err.println("Email sending failed: " + e.getMessage());
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

        // Send account deletion notification email
        emailService.sendAccountDeletionEmail(user.getEmail(), user.getFirstName(), "Suppression par l'administrateur");

        userRepository.deleteById(id);
    }

    // Password reset methods
    public void requestPasswordReset(PasswordResetRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Aucun utilisateur trouvé avec cet email"));

        // Only allow FREELANCER and PROJECT_OWNER to reset passwords
        if (user.getRole() == Role.ADMIN) {
            throw new RuntimeException("Les administrateurs ne peuvent pas réinitialiser leur mot de passe via cette méthode");
        }

        // Generate reset token
        String resetToken = UUID.randomUUID().toString();
        resetTokens.put(resetToken, user.getEmail());
        tokenExpirations.put(resetToken, LocalDateTime.now().plusMinutes(15));

        // Send reset email
        emailService.sendPasswordResetEmail(user.getEmail(), resetToken);
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

        // Send confirmation email
        emailService.sendPasswordResetConfirmationEmail(email);

        return "Mot de passe réinitialisé avec succès";
    }

    // Email verification methods
    public String verifyEmail(EmailVerificationRequest request) {
        // Validate token
        String email = emailVerificationTokens.get(request.getVerificationToken());
        if (email == null) {
            throw new RuntimeException("Token de vérification invalide");
        }

        // Check if token matches the email
        if (!email.equals(request.getEmail())) {
            throw new RuntimeException("Token ne correspond pas à cet email");
        }

        LocalDateTime expiration = emailTokenExpirations.get(request.getVerificationToken());
        if (expiration == null || LocalDateTime.now().isAfter(expiration)) {
            emailVerificationTokens.remove(request.getVerificationToken());
            emailTokenExpirations.remove(request.getVerificationToken());
            throw new RuntimeException("Token de vérification expiré");
        }

        // Find user and enable account
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        user.setEnabled(true);
        userRepository.save(user);

        // Clean up token
        emailVerificationTokens.remove(request.getVerificationToken());
        emailTokenExpirations.remove(request.getVerificationToken());

        return "Email vérifié avec succès. Votre compte est maintenant activé.";
    }

    public String resendVerificationEmail(ResendVerificationRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Aucun utilisateur trouvé avec cet email"));

        if (user.isEnabled()) {
            throw new RuntimeException("Ce compte est déjà vérifié");
        }

        // Generate new verification token
        String verificationToken = UUID.randomUUID().toString();
        emailVerificationTokens.put(verificationToken, user.getEmail());
        emailTokenExpirations.put(verificationToken, LocalDateTime.now().plusHours(24));

        // Send verification email
        emailService.resendVerificationEmail(user.getEmail(), user.getFirstName(), verificationToken);

        return "Un nouvel email de vérification a été envoyé à " + user.getEmail();
    }
}
