package Service;

import DTO.*;
import Entity.Role;
import Entity.User;
import Repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService Tests")
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private SendGridEmailService emailService;

    @InjectMocks
    private UserService userService;

    private User testUser;
    private SignUpRequest signUpRequest;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@example.com")
                .password("encodedPassword")
                .address("123 Main St")
                .birthDate(LocalDate.of(1990, 1, 1))
                .role(Role.FREELANCER)
                .enabled(true)
                .faceDescriptor("face123")
                .profilePictureUrl("/images/profile.jpg")
                .build();

        signUpRequest = new SignUpRequest();
        signUpRequest.setFirstName("John");
        signUpRequest.setLastName("Doe");
        signUpRequest.setEmail("john.doe@example.com");
        signUpRequest.setPassword("password123");
        signUpRequest.setConfirmPassword("password123");
        signUpRequest.setAddress("123 Main St");
        signUpRequest.setBirthDate(LocalDate.of(1990, 1, 1));
        signUpRequest.setRole(Role.FREELANCER);
        signUpRequest.setFaceDescriptor("face123");
    }

    @Nested
    @DisplayName("User Registration Tests")
    class RegistrationTests {

        @Test
        @DisplayName("Should register user successfully")
        void shouldRegisterUserSuccessfully() {
            // Given
            when(userRepository.existsByEmail(signUpRequest.getEmail())).thenReturn(false);
            when(passwordEncoder.encode(signUpRequest.getPassword())).thenReturn("encodedPassword");
            when(userRepository.save(any(User.class))).thenReturn(testUser);
            doNothing().when(emailService).sendWelcomeEmail(anyString(), anyString(), anyString());
            doNothing().when(emailService).sendEmailVerification(anyString(), anyString(), anyString());

            // When
            User result = userService.register(signUpRequest, null);

            // Then
            assertNotNull(result);
            assertEquals(testUser.getEmail(), result.getEmail());
            verify(userRepository).existsByEmail(signUpRequest.getEmail());
            verify(passwordEncoder).encode(signUpRequest.getPassword());
            verify(userRepository).save(any(User.class));
            verify(emailService).sendWelcomeEmail(eq(testUser.getEmail()), eq(testUser.getFirstName()), eq(testUser.getLastName()));
            verify(emailService).sendEmailVerification(eq(testUser.getEmail()), eq(testUser.getFirstName()), anyString());
        }

        @Test
        @DisplayName("Should throw exception when email already exists")
        void shouldThrowExceptionWhenEmailAlreadyExists() {
            // Given
            when(userRepository.existsByEmail(signUpRequest.getEmail())).thenReturn(true);

            // When & Then
            RuntimeException exception = assertThrows(RuntimeException.class, 
                () -> userService.register(signUpRequest, null));
            assertEquals("Cet email est déjà utilisé", exception.getMessage());
            verify(userRepository).existsByEmail(signUpRequest.getEmail());
            verify(userRepository, never()).save(any(User.class));
        }

        @Test
        @DisplayName("Should throw exception when passwords don't match")
        void shouldThrowExceptionWhenPasswordsDontMatch() {
            // Given
            signUpRequest.setConfirmPassword("differentPassword");
            when(userRepository.existsByEmail(signUpRequest.getEmail())).thenReturn(false);

            // When & Then
            RuntimeException exception = assertThrows(RuntimeException.class, 
                () -> userService.register(signUpRequest, null));
            assertEquals("Les mots de passe ne correspondent pas", exception.getMessage());
            verify(userRepository, never()).save(any(User.class));
        }

        @Test
        @DisplayName("Should throw exception when trying to create admin via signup")
        void shouldThrowExceptionWhenTryingToCreateAdmin() {
            // Given
            signUpRequest.setRole(Role.ADMIN);
            when(userRepository.existsByEmail(signUpRequest.getEmail())).thenReturn(false);

            // When & Then
            RuntimeException exception = assertThrows(RuntimeException.class, 
                () -> userService.register(signUpRequest, null));
            assertEquals("Impossible de créer un compte admin via l'inscription", exception.getMessage());
            verify(userRepository, never()).save(any(User.class));
        }

        @Test
        @DisplayName("Should register user with profile picture")
        void shouldRegisterUserWithProfilePicture() {
            // Given
            MockMultipartFile file = new MockMultipartFile("file", "test.jpg", "image/jpeg", "test image content".getBytes());
            when(userRepository.existsByEmail(signUpRequest.getEmail())).thenReturn(false);
            when(passwordEncoder.encode(signUpRequest.getPassword())).thenReturn("encodedPassword");
            when(userRepository.save(any(User.class))).thenReturn(testUser);
            doNothing().when(emailService).sendWelcomeEmail(anyString(), anyString(), anyString());
            doNothing().when(emailService).sendEmailVerification(anyString(), anyString(), anyString());

            // When
            User result = userService.register(signUpRequest, file);

            // Then
            assertNotNull(result);
            verify(userRepository).save(any(User.class));
        }

        @Test
        @DisplayName("Should handle email service failure during registration")
        void shouldHandleEmailServiceFailure() {
            // Given
            when(userRepository.existsByEmail(signUpRequest.getEmail())).thenReturn(false);
            when(passwordEncoder.encode(signUpRequest.getPassword())).thenReturn("encodedPassword");
            when(userRepository.save(any(User.class))).thenReturn(testUser);
            doThrow(new RuntimeException("Email service error")).when(emailService)
                .sendWelcomeEmail(anyString(), anyString(), anyString());

            // When & Then
            RuntimeException exception = assertThrows(RuntimeException.class, 
                () -> userService.register(signUpRequest, null));
            assertTrue(exception.getMessage().contains("Compte créé mais erreur lors de l'envoi de l'email de vérification"));
        }
    }

    @Nested
    @DisplayName("User Management Tests")
    class UserManagementTests {

        @Test
        @DisplayName("Should get all users")
        void shouldGetAllUsers() {
            // Given
            List<User> users = Arrays.asList(testUser);
            when(userRepository.findAll()).thenReturn(users);

            // When
            List<User> result = userService.getAllUsers();

            // Then
            assertNotNull(result);
            assertEquals(1, result.size());
            assertEquals(testUser, result.get(0));
            verify(userRepository).findAll();
        }

        @Test
        @DisplayName("Should delete user successfully")
        void shouldDeleteUserSuccessfully() {
            // Given
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            doNothing().when(emailService).sendAccountDeletionEmail(anyString(), anyString(), anyString());
            doNothing().when(userRepository).deleteById(1L);

            // When
            userService.deleteUser(1L);

            // Then
            verify(userRepository).findById(1L);
            verify(emailService).sendAccountDeletionEmail(eq(testUser.getEmail()), eq(testUser.getFirstName()), anyString());
            verify(userRepository).deleteById(1L);
        }

        @Test
        @DisplayName("Should throw exception when deleting non-existent user")
        void shouldThrowExceptionWhenDeletingNonExistentUser() {
            // Given
            when(userRepository.findById(1L)).thenReturn(Optional.empty());

            // When & Then
            RuntimeException exception = assertThrows(RuntimeException.class, 
                () -> userService.deleteUser(1L));
            assertEquals("Utilisateur non trouvé avec l'id : 1", exception.getMessage());
            verify(userRepository).findById(1L);
            verify(userRepository, never()).deleteById(anyLong());
        }

        @Test
        @DisplayName("Should throw exception when trying to delete admin")
        void shouldThrowExceptionWhenTryingToDeleteAdmin() {
            // Given
            User adminUser = User.builder()
                .id(1L)
                .firstName("Admin")
                .lastName("User")
                .email("admin@example.com")
                .role(Role.ADMIN)
                .build();
            when(userRepository.findById(1L)).thenReturn(Optional.of(adminUser));

            // When & Then
            RuntimeException exception = assertThrows(RuntimeException.class, 
                () -> userService.deleteUser(1L));
            assertEquals("Impossible de supprimer l'administrateur", exception.getMessage());
            verify(userRepository).findById(1L);
            verify(userRepository, never()).deleteById(anyLong());
        }

        @Test
        @DisplayName("Should update user status successfully")
        void shouldUpdateUserStatusSuccessfully() {
            // Given
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(userRepository.save(any(User.class))).thenReturn(testUser);

            // When
            userService.updateUserStatus(1L, false);

            // Then
            verify(userRepository).findById(1L);
            verify(userRepository).save(any(User.class));
        }

        @Test
        @DisplayName("Should throw exception when trying to disable admin")
        void shouldThrowExceptionWhenTryingToDisableAdmin() {
            // Given
            User adminUser = User.builder()
                .id(1L)
                .firstName("Admin")
                .lastName("User")
                .email("admin@example.com")
                .role(Role.ADMIN)
                .build();
            when(userRepository.findById(1L)).thenReturn(Optional.of(adminUser));

            // When & Then
            RuntimeException exception = assertThrows(RuntimeException.class, 
                () -> userService.updateUserStatus(1L, false));
            assertEquals("Impossible de désactiver un utilisateur administrateur", exception.getMessage());
            verify(userRepository).findById(1L);
            verify(userRepository, never()).save(any(User.class));
        }
    }

    @Nested
    @DisplayName("Password Reset Tests")
    class PasswordResetTests {

        @Test
        @DisplayName("Should request password reset successfully")
        void shouldRequestPasswordResetSuccessfully() {
            // Given
            PasswordResetRequest request = new PasswordResetRequest();
            request.setEmail("john.doe@example.com");
            when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.of(testUser));
            doNothing().when(emailService).sendPasswordResetEmailWithCode(anyString(), anyString(), anyString());

            // When
            userService.requestPasswordReset(request);

            // Then
            verify(userRepository).findByEmail(request.getEmail());
            verify(emailService).sendPasswordResetEmailWithCode(eq(testUser.getEmail()), eq(testUser.getFirstName()), anyString());
        }

        @Test
        @DisplayName("Should throw exception when user not found for password reset")
        void shouldThrowExceptionWhenUserNotFoundForPasswordReset() {
            // Given
            PasswordResetRequest request = new PasswordResetRequest();
            request.setEmail("nonexistent@example.com");
            when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.empty());

            // When & Then
            RuntimeException exception = assertThrows(RuntimeException.class, 
                () -> userService.requestPasswordReset(request));
            assertEquals("Aucun utilisateur trouvé avec cet email", exception.getMessage());
            verify(userRepository).findByEmail(request.getEmail());
        }

        @Test
        @DisplayName("Should confirm password reset successfully")
        void shouldConfirmPasswordResetSuccessfully() {
            // Given
            PasswordResetConfirm request = new PasswordResetConfirm();
            request.setToken("validToken");
            request.setNewPassword("newPassword");
            request.setConfirmPassword("newPassword");

            // Setup internal maps using reflection
            Map<String, String> resetTokens = new ConcurrentHashMap<>();
            Map<String, LocalDateTime> tokenExpirations = new ConcurrentHashMap<>();
            resetTokens.put("validToken", "john.doe@example.com");
            tokenExpirations.put("validToken", LocalDateTime.now().plusHours(1));
            
            ReflectionTestUtils.setField(userService, "resetTokens", resetTokens);
            ReflectionTestUtils.setField(userService, "tokenExpirations", tokenExpirations);

            when(userRepository.findByEmail("john.doe@example.com")).thenReturn(Optional.of(testUser));
            when(passwordEncoder.encode("newPassword")).thenReturn("encodedNewPassword");
            when(userRepository.save(any(User.class))).thenReturn(testUser);
            doNothing().when(emailService).sendPasswordResetConfirmationEmail(anyString());

            // When
            String result = userService.confirmPasswordReset(request);

            // Then
            assertEquals("Mot de passe réinitialisé avec succès", result);
            verify(userRepository).findByEmail("john.doe@example.com");
            verify(passwordEncoder).encode("newPassword");
            verify(userRepository).save(any(User.class));
            verify(emailService).sendPasswordResetConfirmationEmail("john.doe@example.com");
        }

        @Test
        @DisplayName("Should throw exception for invalid reset token")
        void shouldThrowExceptionForInvalidResetToken() {
            // Given
            PasswordResetConfirm request = new PasswordResetConfirm();
            request.setToken("invalidToken");
            request.setNewPassword("newPassword");
            request.setConfirmPassword("newPassword");

            // When & Then
            RuntimeException exception = assertThrows(RuntimeException.class, 
                () -> userService.confirmPasswordReset(request));
            assertEquals("Token de réinitialisation invalide", exception.getMessage());
        }

        @Test
        @DisplayName("Should throw exception for expired reset token")
        void shouldThrowExceptionForExpiredResetToken() {
            // Given
            PasswordResetConfirm request = new PasswordResetConfirm();
            request.setToken("expiredToken");
            request.setNewPassword("newPassword");
            request.setConfirmPassword("newPassword");

            // Setup internal maps with expired token
            Map<String, String> resetTokens = new ConcurrentHashMap<>();
            Map<String, LocalDateTime> tokenExpirations = new ConcurrentHashMap<>();
            resetTokens.put("expiredToken", "john.doe@example.com");
            tokenExpirations.put("expiredToken", LocalDateTime.now().minusHours(1));
            
            ReflectionTestUtils.setField(userService, "resetTokens", resetTokens);
            ReflectionTestUtils.setField(userService, "tokenExpirations", tokenExpirations);

            // When & Then
            RuntimeException exception = assertThrows(RuntimeException.class, 
                () -> userService.confirmPasswordReset(request));
            assertEquals("Token de réinitialisation expiré", exception.getMessage());
        }

        @Test
        @DisplayName("Should throw exception when passwords don't match in reset")
        void shouldThrowExceptionWhenPasswordsDontMatchInReset() {
            // Given
            PasswordResetConfirm request = new PasswordResetConfirm();
            request.setToken("validToken");
            request.setNewPassword("newPassword");
            request.setConfirmPassword("differentPassword");

            // Setup internal maps
            Map<String, String> resetTokens = new ConcurrentHashMap<>();
            Map<String, LocalDateTime> tokenExpirations = new ConcurrentHashMap<>();
            resetTokens.put("validToken", "john.doe@example.com");
            tokenExpirations.put("validToken", LocalDateTime.now().plusHours(1));
            
            ReflectionTestUtils.setField(userService, "resetTokens", resetTokens);
            ReflectionTestUtils.setField(userService, "tokenExpirations", tokenExpirations);

            // When & Then
            RuntimeException exception = assertThrows(RuntimeException.class, 
                () -> userService.confirmPasswordReset(request));
            assertEquals("Les mots de passe ne correspondent pas", exception.getMessage());
        }
    }

    @Nested
    @DisplayName("Email Verification Tests")
    class EmailVerificationTests {

        @Test
        @DisplayName("Should verify email successfully")
        void shouldVerifyEmailSuccessfully() {
            // Given
            EmailVerificationRequest request = new EmailVerificationRequest();
            request.setEmail("john.doe@example.com");
            request.setVerificationCode("123456");

            // Setup internal maps
            Map<String, String> emailVerificationCodes = new ConcurrentHashMap<>();
            Map<String, LocalDateTime> emailCodeExpirations = new ConcurrentHashMap<>();
            emailVerificationCodes.put("john.doe@example.com", "123456");
            emailCodeExpirations.put("john.doe@example.com", LocalDateTime.now().plusHours(1));
            
            ReflectionTestUtils.setField(userService, "emailVerificationCodes", emailVerificationCodes);
            ReflectionTestUtils.setField(userService, "emailCodeExpirations", emailCodeExpirations);

            when(userRepository.findByEmail("john.doe@example.com")).thenReturn(Optional.of(testUser));
            when(userRepository.save(any(User.class))).thenReturn(testUser);

            // When
            String result = userService.verifyEmail(request);

            // Then
            assertEquals("Email vérifié avec succès. Votre compte est maintenant activé.", result);
            verify(userRepository).findByEmail("john.doe@example.com");
            verify(userRepository).save(any(User.class));
        }

        @Test
        @DisplayName("Should throw exception for invalid verification code")
        void shouldThrowExceptionForInvalidVerificationCode() {
            // Given
            EmailVerificationRequest request = new EmailVerificationRequest();
            request.setEmail("john.doe@example.com");
            request.setVerificationCode("wrong123");

            // Setup internal maps
            Map<String, String> emailVerificationCodes = new ConcurrentHashMap<>();
            emailVerificationCodes.put("john.doe@example.com", "123456");
            
            ReflectionTestUtils.setField(userService, "emailVerificationCodes", emailVerificationCodes);

            // When & Then
            RuntimeException exception = assertThrows(RuntimeException.class, 
                () -> userService.verifyEmail(request));
            assertEquals("Code de vérification incorrect", exception.getMessage());
        }

        @Test
        @DisplayName("Should resend verification email successfully")
        void shouldResendVerificationEmailSuccessfully() {
            // Given
            ResendVerificationRequest request = new ResendVerificationRequest();
            request.setEmail("john.doe@example.com");
            
            User disabledUser = User.builder()
                .email("john.doe@example.com")
                .firstName("John")
                .enabled(false)
                .build();

            when(userRepository.findByEmail("john.doe@example.com")).thenReturn(Optional.of(disabledUser));
            doNothing().when(emailService).resendVerificationEmail(anyString(), anyString(), anyString());

            // When
            String result = userService.resendVerificationEmail(request);

            // Then
            assertEquals("Un nouvel email de vérification a été envoyé à john.doe@example.com", result);
            verify(userRepository).findByEmail("john.doe@example.com");
            verify(emailService).resendVerificationEmail(eq("john.doe@example.com"), eq("John"), anyString());
        }

        @Test
        @DisplayName("Should throw exception when account already verified")
        void shouldThrowExceptionWhenAccountAlreadyVerified() {
            // Given
            ResendVerificationRequest request = new ResendVerificationRequest();
            request.setEmail("john.doe@example.com");

            when(userRepository.findByEmail("john.doe@example.com")).thenReturn(Optional.of(testUser)); // enabled = true

            // When & Then
            RuntimeException exception = assertThrows(RuntimeException.class, 
                () -> userService.resendVerificationEmail(request));
            assertEquals("Ce compte est déjà vérifié", exception.getMessage());
        }
    }

    @Nested
    @DisplayName("Face Recognition Tests")
    class FaceRecognitionTests {

        @Test
        @DisplayName("Should sign in with face successfully")
        void shouldSignInWithFaceSuccessfully() {
            // Given
            String email = "john.doe@example.com";
            String loginDescriptor = generateFaceDescriptor(0.0); // Same as stored descriptor
            testUser.setFaceDescriptor(generateFaceDescriptor(0.0));

            when(userRepository.findByEmail(email)).thenReturn(Optional.of(testUser));

            // When
            User result = userService.signInWithFace(email, loginDescriptor);

            // Then
            assertNotNull(result);
            assertEquals(testUser, result);
            verify(userRepository).findByEmail(email);
        }

        @Test
        @DisplayName("Should throw exception for disabled user in face login")
        void shouldThrowExceptionForDisabledUserInFaceLogin() {
            // Given
            String email = "john.doe@example.com";
            String loginDescriptor = generateFaceDescriptor(0.0);
            testUser.setEnabled(false);

            when(userRepository.findByEmail(email)).thenReturn(Optional.of(testUser));

            // When & Then
            DisabledException exception = assertThrows(DisabledException.class, 
                () -> userService.signInWithFace(email, loginDescriptor));
            assertTrue(exception.getMessage().contains("Ce compte est désactivé"));
        }

        @Test
        @DisplayName("Should throw exception when face descriptor not configured")
        void shouldThrowExceptionWhenFaceDescriptorNotConfigured() {
            // Given
            String email = "john.doe@example.com";
            String loginDescriptor = generateFaceDescriptor(0.0);
            testUser.setFaceDescriptor(null);

            when(userRepository.findByEmail(email)).thenReturn(Optional.of(testUser));

            // When & Then
            BadCredentialsException exception = assertThrows(BadCredentialsException.class, 
                () -> userService.signInWithFace(email, loginDescriptor));
            assertTrue(exception.getMessage().contains("Le login par reconnaissance faciale n'est pas configuré"));
        }

        @Test
        @DisplayName("Should throw exception when face not recognized")
        void shouldThrowExceptionWhenFaceNotRecognized() {
            // Given
            String email = "john.doe@example.com";
            String loginDescriptor = generateFaceDescriptor(1.0); // Very different descriptor
            testUser.setFaceDescriptor(generateFaceDescriptor(0.0));

            when(userRepository.findByEmail(email)).thenReturn(Optional.of(testUser));

            // When & Then
            BadCredentialsException exception = assertThrows(BadCredentialsException.class, 
                () -> userService.signInWithFace(email, loginDescriptor));
            assertEquals("Visage non reconnu.", exception.getMessage());
        }

        @Test
        @DisplayName("Should throw exception for non-existent user in face login")
        void shouldThrowExceptionForNonExistentUserInFaceLogin() {
            // Given
            String email = "nonexistent@example.com";
            String loginDescriptor = generateFaceDescriptor(0.0);

            when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

            // When & Then
            BadCredentialsException exception = assertThrows(BadCredentialsException.class, 
                () -> userService.signInWithFace(email, loginDescriptor));
            assertEquals("Email ou visage incorrect", exception.getMessage());
        }

        private String generateFaceDescriptor(double baseValue) {
            StringBuilder descriptor = new StringBuilder();
            for (int i = 0; i < 128; i++) {
                if (i > 0) descriptor.append(",");
                descriptor.append(String.format("%.6f", baseValue + (i * 0.001)));
            }
            return descriptor.toString();
        }
    }

    @Nested
    @DisplayName("Password Change Tests")
    class PasswordChangeTests {

        @Test
        @DisplayName("Should change password with code successfully")
        void shouldChangePasswordWithCodeSuccessfully() {
            // Given
            PasswordChangeRequest request = new PasswordChangeRequest();
            request.setEmail("john.doe@example.com");
            request.setVerificationCode("123456");
            request.setOldPassword("oldPassword");
            request.setNewPassword("newPassword");

            // Setup internal maps
            Map<String, String> emailVerificationCodes = new ConcurrentHashMap<>();
            Map<String, LocalDateTime> emailCodeExpirations = new ConcurrentHashMap<>();
            emailVerificationCodes.put("john.doe@example.com", "123456");
            emailCodeExpirations.put("john.doe@example.com", LocalDateTime.now().plusHours(1));
            
            ReflectionTestUtils.setField(userService, "emailVerificationCodes", emailVerificationCodes);
            ReflectionTestUtils.setField(userService, "emailCodeExpirations", emailCodeExpirations);

            when(userRepository.findByEmail("john.doe@example.com")).thenReturn(Optional.of(testUser));
            when(passwordEncoder.matches("oldPassword", testUser.getPassword())).thenReturn(true);
            when(passwordEncoder.encode("newPassword")).thenReturn("encodedNewPassword");
            when(userRepository.save(any(User.class))).thenReturn(testUser);

            // When
            String result = userService.changePasswordWithCode(request);

            // Then
            assertEquals("Mot de passe changé avec succès", result);
            verify(userRepository).findByEmail("john.doe@example.com");
            verify(passwordEncoder).matches("oldPassword", testUser.getPassword());
            verify(passwordEncoder).encode("newPassword");
            verify(userRepository).save(any(User.class));
        }

        @Test
        @DisplayName("Should throw exception for incorrect old password")
        void shouldThrowExceptionForIncorrectOldPassword() {
            // Given
            PasswordChangeRequest request = new PasswordChangeRequest();
            request.setEmail("john.doe@example.com");
            request.setVerificationCode("123456");
            request.setOldPassword("wrongOldPassword");
            request.setNewPassword("newPassword");

            // Setup internal maps
            Map<String, String> emailVerificationCodes = new ConcurrentHashMap<>();
            Map<String, LocalDateTime> emailCodeExpirations = new ConcurrentHashMap<>();
            emailVerificationCodes.put("john.doe@example.com", "123456");
            emailCodeExpirations.put("john.doe@example.com", LocalDateTime.now().plusHours(1));
            
            ReflectionTestUtils.setField(userService, "emailVerificationCodes", emailVerificationCodes);
            ReflectionTestUtils.setField(userService, "emailCodeExpirations", emailCodeExpirations);

            when(userRepository.findByEmail("john.doe@example.com")).thenReturn(Optional.of(testUser));
            when(passwordEncoder.matches("wrongOldPassword", testUser.getPassword())).thenReturn(false);

            // When & Then
            RuntimeException exception = assertThrows(RuntimeException.class, 
                () -> userService.changePasswordWithCode(request));
            assertEquals("Ancien mot de passe incorrect", exception.getMessage());
        }
    }
}