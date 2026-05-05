package Controller;

import DTO.*;
import Entity.Role;
import Entity.User;
import Security.JwtUtils;
import Service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@ActiveProfiles("test")
@DisplayName("AuthController Tests")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @MockBean
    private AuthenticationManager authenticationManager;

    @MockBean
    private JwtUtils jwtUtils;

    @Autowired
    private ObjectMapper objectMapper;

    private User testUser;
    private SignInRequest signInRequest;
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
                .build();

        signInRequest = new SignInRequest();
        signInRequest.setEmail("john.doe@example.com");
        signInRequest.setPassword("password123");

        signUpRequest = new SignUpRequest();
        signUpRequest.setFirstName("John");
        signUpRequest.setLastName("Doe");
        signUpRequest.setEmail("john.doe@example.com");
        signUpRequest.setPassword("password123");
        signUpRequest.setConfirmPassword("password123");
        signUpRequest.setAddress("123 Main St");
        signUpRequest.setBirthDate(LocalDate.of(1990, 1, 1));
        signUpRequest.setRole(Role.FREELANCER);
    }

    @Nested
    @DisplayName("Sign Up Tests")
    class SignUpTests {

        @Test
        @WithMockUser
        @DisplayName("Should sign up user successfully")
        void shouldSignUpUserSuccessfully() throws Exception {
            // Given
            when(userService.register(any(SignUpRequest.class), any())).thenReturn(testUser);

            // When & Then
            mockMvc.perform(multipart("/api/auth/signup")
                    .param("firstName", "John")
                    .param("lastName", "Doe")
                    .param("email", "john.doe@example.com")
                    .param("password", "password123")
                    .param("address", "123 Main St")
                    .param("birthDate", "1990-01-01")
                    .param("role", "FREELANCER")
                    .with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(content().string("Compte créé avec succès pour : john.doe@example.com"));

            verify(userService).register(any(SignUpRequest.class), any());
        }

        @Test
        @WithMockUser
        @DisplayName("Should sign up user with file successfully")
        void shouldSignUpUserWithFileSuccessfully() throws Exception {
            // Given
            MockMultipartFile file = new MockMultipartFile("file", "test.jpg", "image/jpeg", "test image content".getBytes());
            when(userService.register(any(SignUpRequest.class), any())).thenReturn(testUser);

            // When & Then
            mockMvc.perform(multipart("/api/auth/signup")
                    .file(file)
                    .param("firstName", "John")
                    .param("lastName", "Doe")
                    .param("email", "john.doe@example.com")
                    .param("password", "password123")
                    .param("address", "123 Main St")
                    .param("birthDate", "1990-01-01")
                    .param("role", "FREELANCER")
                    .param("faceDescriptor", "face123")
                    .with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(content().string("Compte créé avec succès pour : john.doe@example.com"));

            verify(userService).register(any(SignUpRequest.class), any());
        }

        @Test
        @WithMockUser
        @DisplayName("Should handle sign up error")
        void shouldHandleSignUpError() throws Exception {
            // Given
            when(userService.register(any(SignUpRequest.class), any()))
                    .thenThrow(new RuntimeException("Email already exists"));

            // When & Then
            mockMvc.perform(multipart("/api/auth/signup")
                    .param("firstName", "John")
                    .param("lastName", "Doe")
                    .param("email", "john.doe@example.com")
                    .param("password", "password123")
                    .param("address", "123 Main St")
                    .param("birthDate", "1990-01-01")
                    .param("role", "FREELANCER")
                    .with(csrf()))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().string("Erreur : Email already exists"));

            verify(userService).register(any(SignUpRequest.class), any());
        }

        @Test
        @WithMockUser
        @DisplayName("Should handle invalid date format")
        void shouldHandleInvalidDateFormat() throws Exception {
            // When & Then
            mockMvc.perform(multipart("/api/auth/signup")
                    .param("firstName", "John")
                    .param("lastName", "Doe")
                    .param("email", "john.doe@example.com")
                    .param("password", "password123")
                    .param("address", "123 Main St")
                    .param("birthDate", "invalid-date")
                    .param("role", "FREELANCER")
                    .with(csrf()))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().string(containsString("Erreur :")));

            verify(userService, never()).register(any(SignUpRequest.class), any());
        }

        @Test
        @WithMockUser
        @DisplayName("Should handle invalid role")
        void shouldHandleInvalidRole() throws Exception {
            // When & Then
            mockMvc.perform(multipart("/api/auth/signup")
                    .param("firstName", "John")
                    .param("lastName", "Doe")
                    .param("email", "john.doe@example.com")
                    .param("password", "password123")
                    .param("address", "123 Main St")
                    .param("birthDate", "1990-01-01")
                    .param("role", "INVALID_ROLE")
                    .with(csrf()))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().string(containsString("Erreur :")));

            verify(userService, never()).register(any(SignUpRequest.class), any());
        }
    }

    @Nested
    @DisplayName("Sign In Tests")
    class SignInTests {

        @Test
        @WithMockUser
        @DisplayName("Should sign in user successfully")
        void shouldSignInUserSuccessfully() throws Exception {
            // Given
            Authentication authentication = new UsernamePasswordAuthenticationToken(
                    "john.doe@example.com", 
                    "password123",
                    Collections.singletonList(new SimpleGrantedAuthority("ROLE_FREELANCER"))
            );
            
            when(authenticationManager.authenticate(any())).thenReturn(authentication);
            when(jwtUtils.generateToken("john.doe@example.com", "FREELANCER")).thenReturn("jwt-token");

            // When & Then
            mockMvc.perform(post("/api/auth/signin")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(signInRequest))
                    .with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.token").value("jwt-token"))
                    .andExpect(jsonPath("$.email").value("john.doe@example.com"))
                    .andExpect(jsonPath("$.role").value("FREELANCER"))
                    .andExpected(jsonPath("$.message").value("Redirection vers le Dashboard Freelancer"));

            verify(authenticationManager).authenticate(any());
            verify(jwtUtils).generateToken("john.doe@example.com", "FREELANCER");
        }

        @Test
        @WithMockUser
        @DisplayName("Should sign in admin user successfully")
        void shouldSignInAdminUserSuccessfully() throws Exception {
            // Given
            Authentication authentication = new UsernamePasswordAuthenticationToken(
                    "admin@example.com", 
                    "password123",
                    Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN"))
            );
            
            signInRequest.setEmail("admin@example.com");
            when(authenticationManager.authenticate(any())).thenReturn(authentication);
            when(jwtUtils.generateToken("admin@example.com", "ADMIN")).thenReturn("admin-jwt-token");

            // When & Then
            mockMvc.perform(post("/api/auth/signin")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(signInRequest))
                    .with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.token").value("admin-jwt-token"))
                    .andExpect(jsonPath("$.email").value("admin@example.com"))
                    .andExpect(jsonPath("$.role").value("ADMIN"))
                    .andExpect(jsonPath("$.message").value("Redirection vers le Dashboard Admin"));

            verify(authenticationManager).authenticate(any());
            verify(jwtUtils).generateToken("admin@example.com", "ADMIN");
        }

        @Test
        @WithMockUser
        @DisplayName("Should handle bad credentials")
        void shouldHandleBadCredentials() throws Exception {
            // Given
            when(authenticationManager.authenticate(any()))
                    .thenThrow(new BadCredentialsException("Bad credentials"));

            // When & Then
            mockMvc.perform(post("/api/auth/signin")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(signInRequest))
                    .with(csrf()))
                    .andExpect(status().isUnauthorized())
                    .andExpect(content().string("Email ou mot de passe incorrect"));

            verify(authenticationManager).authenticate(any());
            verify(jwtUtils, never()).generateToken(anyString(), anyString());
        }

        @Test
        @WithMockUser
        @DisplayName("Should handle disabled account")
        void shouldHandleDisabledAccount() throws Exception {
            // Given
            when(authenticationManager.authenticate(any()))
                    .thenThrow(new DisabledException("Account disabled"));

            // When & Then
            mockMvc.perform(post("/api/auth/signin")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(signInRequest))
                    .with(csrf()))
                    .andExpect(status().isForbidden())
                    .andExpect(content().string("Ce compte est désactivé. Veuillez vérifier votre email pour activer votre compte."));

            verify(authenticationManager).authenticate(any());
            verify(jwtUtils, never()).generateToken(anyString(), anyString());
        }
    }

    @Nested
    @DisplayName("Face Sign In Tests")
    class FaceSignInTests {

        @Test
        @WithMockUser
        @DisplayName("Should sign in with face successfully")
        void shouldSignInWithFaceSuccessfully() throws Exception {
            // Given
            FaceSignInRequest faceRequest = new FaceSignInRequest();
            faceRequest.setEmail("john.doe@example.com");
            faceRequest.setFaceDescriptor("face123");

            when(userService.signInWithFace("john.doe@example.com", "face123")).thenReturn(testUser);
            when(jwtUtils.generateToken("john.doe@example.com", "FREELANCER")).thenReturn("face-jwt-token");

            // When & Then
            mockMvc.perform(post("/api/auth/signin-face")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(faceRequest))
                    .with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.token").value("face-jwt-token"))
                    .andExpect(jsonPath("$.email").value("john.doe@example.com"))
                    .andExpect(jsonPath("$.role").value("FREELANCER"));

            verify(userService).signInWithFace("john.doe@example.com", "face123");
            verify(jwtUtils).generateToken("john.doe@example.com", "FREELANCER");
        }

        @Test
        @WithMockUser
        @DisplayName("Should handle face recognition failure")
        void shouldHandleFaceRecognitionFailure() throws Exception {
            // Given
            FaceSignInRequest faceRequest = new FaceSignInRequest();
            faceRequest.setEmail("john.doe@example.com");
            faceRequest.setFaceDescriptor("wrong-face");

            when(userService.signInWithFace("john.doe@example.com", "wrong-face"))
                    .thenThrow(new BadCredentialsException("Face not recognized"));

            // When & Then
            mockMvc.perform(post("/api/auth/signin-face")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(faceRequest))
                    .with(csrf()))
                    .andExpect(status().isUnauthorized())
                    .andExpect(content().string("Face not recognized"));

            verify(userService).signInWithFace("john.doe@example.com", "wrong-face");
            verify(jwtUtils, never()).generateToken(anyString(), anyString());
        }

        @Test
        @WithMockUser
        @DisplayName("Should handle disabled account in face login")
        void shouldHandleDisabledAccountInFaceLogin() throws Exception {
            // Given
            FaceSignInRequest faceRequest = new FaceSignInRequest();
            faceRequest.setEmail("john.doe@example.com");
            faceRequest.setFaceDescriptor("face123");

            when(userService.signInWithFace("john.doe@example.com", "face123"))
                    .thenThrow(new DisabledException("Account disabled"));

            // When & Then
            mockMvc.perform(post("/api/auth/signin-face")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(faceRequest))
                    .with(csrf()))
                    .andExpect(status().isForbidden())
                    .andExpect(content().string("Account disabled"));

            verify(userService).signInWithFace("john.doe@example.com", "face123");
            verify(jwtUtils, never()).generateToken(anyString(), anyString());
        }
    }

    @Nested
    @DisplayName("Password Reset Tests")
    class PasswordResetTests {

        @Test
        @WithMockUser
        @DisplayName("Should request password reset successfully")
        void shouldRequestPasswordResetSuccessfully() throws Exception {
            // Given
            PasswordResetRequest resetRequest = new PasswordResetRequest();
            resetRequest.setEmail("john.doe@example.com");

            doNothing().when(userService).requestPasswordReset(any(PasswordResetRequest.class));

            // When & Then
            mockMvc.perform(post("/api/auth/reset-password/request")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(resetRequest))
                    .with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(content().string("Un email de réinitialisation a été envoyé à john.doe@example.com"));

            verify(userService).requestPasswordReset(any(PasswordResetRequest.class));
        }

        @Test
        @WithMockUser
        @DisplayName("Should handle password reset request error")
        void shouldHandlePasswordResetRequestError() throws Exception {
            // Given
            PasswordResetRequest resetRequest = new PasswordResetRequest();
            resetRequest.setEmail("nonexistent@example.com");

            doThrow(new RuntimeException("User not found")).when(userService)
                    .requestPasswordReset(any(PasswordResetRequest.class));

            // When & Then
            mockMvc.perform(post("/api/auth/reset-password/request")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(resetRequest))
                    .with(csrf()))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().string("User not found"));

            verify(userService).requestPasswordReset(any(PasswordResetRequest.class));
        }

        @Test
        @WithMockUser
        @DisplayName("Should confirm password reset successfully")
        void shouldConfirmPasswordResetSuccessfully() throws Exception {
            // Given
            PasswordResetConfirm confirmRequest = new PasswordResetConfirm();
            confirmRequest.setToken("valid-token");
            confirmRequest.setNewPassword("newPassword123");
            confirmRequest.setConfirmPassword("newPassword123");

            when(userService.confirmPasswordReset(any(PasswordResetConfirm.class)))
                    .thenReturn("Password reset successfully");

            // When & Then
            mockMvc.perform(post("/api/auth/reset-password/confirm")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(confirmRequest))
                    .with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(content().string("Password reset successfully"));

            verify(userService).confirmPasswordReset(any(PasswordResetConfirm.class));
        }

        @Test
        @WithMockUser
        @DisplayName("Should handle invalid reset token")
        void shouldHandleInvalidResetToken() throws Exception {
            // Given
            PasswordResetConfirm confirmRequest = new PasswordResetConfirm();
            confirmRequest.setToken("invalid-token");
            confirmRequest.setNewPassword("newPassword123");
            confirmRequest.setConfirmPassword("newPassword123");

            when(userService.confirmPasswordReset(any(PasswordResetConfirm.class)))
                    .thenThrow(new RuntimeException("Invalid token"));

            // When & Then
            mockMvc.perform(post("/api/auth/reset-password/confirm")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(confirmRequest))
                    .with(csrf()))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().string("Invalid token"));

            verify(userService).confirmPasswordReset(any(PasswordResetConfirm.class));
        }

        @Test
        @WithMockUser
        @DisplayName("Should show reset form")
        void shouldShowResetForm() throws Exception {
            // When & Then
            mockMvc.perform(get("/api/auth/reset-password/confirm")
                    .param("token", "some-token")
                    .with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(content().string(containsString("Token valide")));
        }
    }

    @Nested
    @DisplayName("Email Verification Tests")
    class EmailVerificationTests {

        @Test
        @WithMockUser
        @DisplayName("Should verify email successfully")
        void shouldVerifyEmailSuccessfully() throws Exception {
            // Given
            EmailVerificationRequest verifyRequest = new EmailVerificationRequest();
            verifyRequest.setEmail("john.doe@example.com");
            verifyRequest.setVerificationCode("123456");

            when(userService.verifyEmail(any(EmailVerificationRequest.class)))
                    .thenReturn("Email verified successfully");

            // When & Then
            mockMvc.perform(post("/api/auth/verify-email")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(verifyRequest))
                    .with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(content().string("Email verified successfully"));

            verify(userService).verifyEmail(any(EmailVerificationRequest.class));
        }

        @Test
        @WithMockUser
        @DisplayName("Should handle invalid verification code")
        void shouldHandleInvalidVerificationCode() throws Exception {
            // Given
            EmailVerificationRequest verifyRequest = new EmailVerificationRequest();
            verifyRequest.setEmail("john.doe@example.com");
            verifyRequest.setVerificationCode("wrong-code");

            when(userService.verifyEmail(any(EmailVerificationRequest.class)))
                    .thenThrow(new RuntimeException("Invalid verification code"));

            // When & Then
            mockMvc.perform(post("/api/auth/verify-email")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(verifyRequest))
                    .with(csrf()))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().string("Invalid verification code"));

            verify(userService).verifyEmail(any(EmailVerificationRequest.class));
        }

        @Test
        @WithMockUser
        @DisplayName("Should show verification form")
        void shouldShowVerificationForm() throws Exception {
            // When & Then
            mockMvc.perform(get("/api/auth/verify-email")
                    .param("token", "some-token")
                    .param("email", "john.doe@example.com")
                    .with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(content().string(containsString("Token valide")));
        }

        @Test
        @WithMockUser
        @DisplayName("Should resend verification email successfully")
        void shouldResendVerificationEmailSuccessfully() throws Exception {
            // Given
            ResendVerificationRequest resendRequest = new ResendVerificationRequest();
            resendRequest.setEmail("john.doe@example.com");

            when(userService.resendVerificationEmail(any(ResendVerificationRequest.class)))
                    .thenReturn("Verification email sent");

            // When & Then
            mockMvc.perform(post("/api/auth/resend-verification")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(resendRequest))
                    .with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(content().string("Verification email sent"));

            verify(userService).resendVerificationEmail(any(ResendVerificationRequest.class));
        }
    }

    @Nested
    @DisplayName("Password Change Tests")
    class PasswordChangeTests {

        @Test
        @WithMockUser
        @DisplayName("Should change password with code successfully")
        void shouldChangePasswordWithCodeSuccessfully() throws Exception {
            // Given
            PasswordChangeRequest changeRequest = new PasswordChangeRequest();
            changeRequest.setEmail("john.doe@example.com");
            changeRequest.setVerificationCode("123456");
            changeRequest.setOldPassword("oldPassword");
            changeRequest.setNewPassword("newPassword");

            when(userService.changePasswordWithCode(any(PasswordChangeRequest.class)))
                    .thenReturn("Password changed successfully");

            // When & Then
            mockMvc.perform(post("/api/auth/change-password-with-code")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(changeRequest))
                    .with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(content().string("Password changed successfully"));

            verify(userService).changePasswordWithCode(any(PasswordChangeRequest.class));
        }

        @Test
        @WithMockUser
        @DisplayName("Should handle password change error")
        void shouldHandlePasswordChangeError() throws Exception {
            // Given
            PasswordChangeRequest changeRequest = new PasswordChangeRequest();
            changeRequest.setEmail("john.doe@example.com");
            changeRequest.setVerificationCode("wrong-code");
            changeRequest.setOldPassword("oldPassword");
            changeRequest.setNewPassword("newPassword");

            when(userService.changePasswordWithCode(any(PasswordChangeRequest.class)))
                    .thenThrow(new RuntimeException("Invalid verification code"));

            // When & Then
            mockMvc.perform(post("/api/auth/change-password-with-code")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(changeRequest))
                    .with(csrf()))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().string("Invalid verification code"));

            verify(userService).changePasswordWithCode(any(PasswordChangeRequest.class));
        }
    }
}