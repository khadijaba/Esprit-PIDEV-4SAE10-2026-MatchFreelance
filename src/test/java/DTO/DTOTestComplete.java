package DTO;

import Entity.Role;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

@DisplayName("DTO Complete Tests")
class DTOTestComplete {

    @Nested
    @DisplayName("JwtResponse Tests")
    class JwtResponseTests {

        @Test
        @DisplayName("Should create JwtResponse with default constructor")
        void shouldCreateJwtResponseWithDefaultConstructor() {
            JwtResponse response = new JwtResponse();
            assertNotNull(response);
            assertNull(response.getToken());
            assertNull(response.getEmail());
            assertNull(response.getRole());
            assertNull(response.getMessage());
        }

        @Test
        @DisplayName("Should create JwtResponse with parameterized constructor")
        void shouldCreateJwtResponseWithParameterizedConstructor() {
            String token = "jwt.token.here";
            String email = "user@example.com";
            Role role = Role.FREELANCER;
            String message = "Login successful";

            JwtResponse response = new JwtResponse(token, email, role, message);

            assertEquals(token, response.getToken());
            assertEquals(email, response.getEmail());
            assertEquals(role, response.getRole());
            assertEquals(message, response.getMessage());
        }

        @Test
        @DisplayName("Should set and get all properties correctly")
        void shouldSetAndGetAllProperties() {
            JwtResponse response = new JwtResponse();
            String token = "new.jwt.token";
            String email = "new@example.com";
            Role role = Role.PROJECT_OWNER;
            String message = "Updated successfully";

            response.setToken(token);
            response.setEmail(email);
            response.setRole(role);
            response.setMessage(message);

            assertEquals(token, response.getToken());
            assertEquals(email, response.getEmail());
            assertEquals(role, response.getRole());
            assertEquals(message, response.getMessage());
        }

        @Test
        @DisplayName("Should handle null values correctly")
        void shouldHandleNullValues() {
            JwtResponse response = new JwtResponse();
            response.setToken(null);
            response.setEmail(null);
            response.setRole(null);
            response.setMessage(null);

            assertNull(response.getToken());
            assertNull(response.getEmail());
            assertNull(response.getRole());
            assertNull(response.getMessage());
        }
    }

    @Nested
    @DisplayName("SignUpRequest Tests")
    class SignUpRequestTests {

        @Test
        @DisplayName("Should create SignUpRequest with default constructor")
        void shouldCreateSignUpRequestWithDefaultConstructor() {
            SignUpRequest request = new SignUpRequest();
            assertNotNull(request);
            assertNull(request.getFirstName());
            assertNull(request.getLastName());
            assertNull(request.getEmail());
        }

        @Test
        @DisplayName("Should set and get all properties correctly")
        void shouldSetAndGetAllProperties() {
            SignUpRequest request = new SignUpRequest();
            String firstName = "John";
            String lastName = "Doe";
            String address = "123 Main St";
            String email = "john.doe@example.com";
            String password = "password123";
            LocalDate birthDate = LocalDate.of(1990, 1, 1);
            Role role = Role.FREELANCER;

            request.setFirstName(firstName);
            request.setLastName(lastName);
            request.setAddress(address);
            request.setEmail(email);
            request.setPassword(password);
            request.setBirthDate(birthDate);
            request.setRole(role);

            assertEquals(firstName, request.getFirstName());
            assertEquals(lastName, request.getLastName());
            assertEquals(address, request.getAddress());
            assertEquals(email, request.getEmail());
            assertEquals(password, request.getPassword());
            assertEquals(birthDate, request.getBirthDate());
            assertEquals(role, request.getRole());
        }

        @Test
        @DisplayName("Should handle different roles")
        void shouldHandleDifferentRoles() {
            SignUpRequest request = new SignUpRequest();
            
            request.setRole(Role.ADMIN);
            assertEquals(Role.ADMIN, request.getRole());

            request.setRole(Role.PROJECT_OWNER);
            assertEquals(Role.PROJECT_OWNER, request.getRole());

            request.setRole(Role.FREELANCER);
            assertEquals(Role.FREELANCER, request.getRole());
        }
    }

    @Nested
    @DisplayName("SignInRequest Tests")
    class SignInRequestTests {

        @Test
        @DisplayName("Should create SignInRequest with default constructor")
        void shouldCreateSignInRequestWithDefaultConstructor() {
            SignInRequest request = new SignInRequest();
            assertNotNull(request);
            assertNull(request.getEmail());
            assertNull(request.getPassword());
        }

        @Test
        @DisplayName("Should set and get all properties correctly")
        void shouldSetAndGetAllProperties() {
            SignInRequest request = new SignInRequest();
            String email = "user@example.com";
            String password = "password123";

            request.setEmail(email);
            request.setPassword(password);

            assertEquals(email, request.getEmail());
            assertEquals(password, request.getPassword());
        }

        @Test
        @DisplayName("Should handle null values correctly")
        void shouldHandleNullValues() {
            SignInRequest request = new SignInRequest();
            request.setEmail(null);
            request.setPassword(null);

            assertNull(request.getEmail());
            assertNull(request.getPassword());
        }
    }

    @Nested
    @DisplayName("PageResponse Tests")
    class PageResponseTests {

        @Test
        @DisplayName("Should create PageResponse with default constructor")
        void shouldCreatePageResponseWithDefaultConstructor() {
            PageResponse<String> response = new PageResponse<>();
            assertNotNull(response);
            assertNull(response.getContent());
            assertEquals(0, response.getPage());
            assertEquals(0, response.getSize());
        }

        @Test
        @DisplayName("Should create PageResponse with parameterized constructor")
        void shouldCreatePageResponseWithParameterizedConstructor() {
            List<String> content = Arrays.asList("item1", "item2", "item3");
            int page = 0;
            int size = 10;
            long totalElements = 25;

            PageResponse<String> response = new PageResponse<>(content, page, size, totalElements);

            assertEquals(content, response.getContent());
            assertEquals(page, response.getPage());
            assertEquals(size, response.getSize());
            assertEquals(totalElements, response.getTotalElements());
            assertEquals(3, response.getTotalPages());
            assertTrue(response.isFirst());
            assertFalse(response.isLast());
        }

        @Test
        @DisplayName("Should calculate totalPages correctly")
        void shouldCalculateTotalPagesCorrectly() {
            PageResponse<String> response1 = new PageResponse<>(Arrays.asList("item1"), 0, 10, 25);
            assertEquals(3, response1.getTotalPages());

            PageResponse<String> response2 = new PageResponse<>(Arrays.asList("item1"), 0, 5, 23);
            assertEquals(5, response2.getTotalPages());
        }

        @Test
        @DisplayName("Should determine first and last page correctly")
        void shouldDetermineFirstAndLastPageCorrectly() {
            PageResponse<String> firstPage = new PageResponse<>(Arrays.asList("item1"), 0, 10, 25);
            assertTrue(firstPage.isFirst());
            assertFalse(firstPage.isLast());

            PageResponse<String> lastPage = new PageResponse<>(Arrays.asList("item1"), 2, 10, 25);
            assertFalse(lastPage.isFirst());
            assertTrue(lastPage.isLast());
        }
    }

    @Nested
    @DisplayName("PasswordChangeRequest Tests")
    class PasswordChangeRequestTests {

        @Test
        @DisplayName("Should create PasswordChangeRequest with default constructor")
        void shouldCreatePasswordChangeRequestWithDefaultConstructor() {
            PasswordChangeRequest request = new PasswordChangeRequest();
            assertNotNull(request);
            assertNull(request.getEmail());
            assertNull(request.getOldPassword());
            assertNull(request.getNewPassword());
            assertNull(request.getVerificationCode());
        }

        @Test
        @DisplayName("Should set and get all properties correctly")
        void shouldSetAndGetAllProperties() {
            PasswordChangeRequest request = new PasswordChangeRequest();
            String email = "user@example.com";
            String oldPassword = "oldPassword123";
            String newPassword = "newPassword456";
            String verificationCode = "123456";

            request.setEmail(email);
            request.setOldPassword(oldPassword);
            request.setNewPassword(newPassword);
            request.setVerificationCode(verificationCode);

            assertEquals(email, request.getEmail());
            assertEquals(oldPassword, request.getOldPassword());
            assertEquals(newPassword, request.getNewPassword());
            assertEquals(verificationCode, request.getVerificationCode());
        }
    }

    @Nested
    @DisplayName("PasswordResetRequest Tests")
    class PasswordResetRequestTests {

        @Test
        @DisplayName("Should create PasswordResetRequest with default constructor")
        void shouldCreatePasswordResetRequestWithDefaultConstructor() {
            PasswordResetRequest request = new PasswordResetRequest();
            assertNotNull(request);
            assertNull(request.getEmail());
        }

        @Test
        @DisplayName("Should set and get email correctly")
        void shouldSetAndGetEmailCorrectly() {
            PasswordResetRequest request = new PasswordResetRequest();
            String email = "user@example.com";

            request.setEmail(email);

            assertEquals(email, request.getEmail());
        }
    }

    @Nested
    @DisplayName("EmailVerificationRequest Tests")
    class EmailVerificationRequestTests {

        @Test
        @DisplayName("Should create EmailVerificationRequest with default constructor")
        void shouldCreateEmailVerificationRequestWithDefaultConstructor() {
            EmailVerificationRequest request = new EmailVerificationRequest();
            assertNotNull(request);
            assertNull(request.getEmail());
            assertNull(request.getVerificationCode());
        }

        @Test
        @DisplayName("Should set and get all properties correctly")
        void shouldSetAndGetAllProperties() {
            EmailVerificationRequest request = new EmailVerificationRequest();
            String email = "user@example.com";
            String verificationCode = "ABC123";

            request.setEmail(email);
            request.setVerificationCode(verificationCode);

            assertEquals(email, request.getEmail());
            assertEquals(verificationCode, request.getVerificationCode());
        }
    }

    @Nested
    @DisplayName("FaceSignInRequest Tests")
    class FaceSignInRequestTests {

        @Test
        @DisplayName("Should create FaceSignInRequest with default constructor")
        void shouldCreateFaceSignInRequestWithDefaultConstructor() {
            FaceSignInRequest request = new FaceSignInRequest();
            assertNotNull(request);
            assertNull(request.getEmail());
            assertNull(request.getFaceDescriptor());
        }

        @Test
        @DisplayName("Should set and get all properties correctly")
        void shouldSetAndGetAllProperties() {
            FaceSignInRequest request = new FaceSignInRequest();
            String email = "user@example.com";
            String faceDescriptor = "face_descriptor_data_123";

            request.setEmail(email);
            request.setFaceDescriptor(faceDescriptor);

            assertEquals(email, request.getEmail());
            assertEquals(faceDescriptor, request.getFaceDescriptor());
        }
    }

    @Nested
    @DisplayName("PasswordResetConfirm Tests")
    class PasswordResetConfirmTests {

        @Test
        @DisplayName("Should create PasswordResetConfirm with default constructor")
        void shouldCreatePasswordResetConfirmWithDefaultConstructor() {
            PasswordResetConfirm request = new PasswordResetConfirm();
            assertNotNull(request);
            assertNull(request.getToken());
            assertNull(request.getNewPassword());
            assertNull(request.getConfirmPassword());
        }

        @Test
        @DisplayName("Should set and get all properties correctly")
        void shouldSetAndGetAllProperties() {
            PasswordResetConfirm request = new PasswordResetConfirm();
            String token = "reset_token_123456";
            String newPassword = "newPassword123";
            String confirmPassword = "newPassword123";

            request.setToken(token);
            request.setNewPassword(newPassword);
            request.setConfirmPassword(confirmPassword);

            assertEquals(token, request.getToken());
            assertEquals(newPassword, request.getNewPassword());
            assertEquals(confirmPassword, request.getConfirmPassword());
        }
    }

    @Nested
    @DisplayName("ResendVerificationRequest Tests")
    class ResendVerificationRequestTests {

        @Test
        @DisplayName("Should create ResendVerificationRequest with default constructor")
        void shouldCreateResendVerificationRequestWithDefaultConstructor() {
            ResendVerificationRequest request = new ResendVerificationRequest();
            assertNotNull(request);
            assertNull(request.getEmail());
        }

        @Test
        @DisplayName("Should set and get email correctly")
        void shouldSetAndGetEmailCorrectly() {
            ResendVerificationRequest request = new ResendVerificationRequest();
            String email = "user@example.com";

            request.setEmail(email);

            assertEquals(email, request.getEmail());
        }
    }
}