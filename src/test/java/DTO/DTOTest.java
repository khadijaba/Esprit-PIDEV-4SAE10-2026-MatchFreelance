package DTO;

import Entity.Role;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("DTO Tests")
class DTOTest {

    @Nested
    @DisplayName("SignUpRequest Tests")
    class SignUpRequestTests {

        @Test
        @DisplayName("Should create SignUpRequest with all fields")
        void shouldCreateSignUpRequestWithAllFields() {
            // Given
            SignUpRequest request = new SignUpRequest();

            // When
            request.setFirstName("John");
            request.setLastName("Doe");
            request.setEmail("john.doe@example.com");
            request.setPassword("password123");
            request.setConfirmPassword("password123");
            request.setAddress("123 Main St");
            request.setBirthDate(LocalDate.of(1990, 1, 1));
            request.setRole(Role.FREELANCER);
            request.setFaceDescriptor("face123");

            // Then
            assertEquals("John", request.getFirstName());
            assertEquals("Doe", request.getLastName());
            assertEquals("john.doe@example.com", request.getEmail());
            assertEquals("password123", request.getPassword());
            assertEquals("password123", request.getConfirmPassword());
            assertEquals("123 Main St", request.getAddress());
            assertEquals(LocalDate.of(1990, 1, 1), request.getBirthDate());
            assertEquals(Role.FREELANCER, request.getRole());
            assertEquals("face123", request.getFaceDescriptor());
        }

        @Test
        @DisplayName("Should handle null values")
        void shouldHandleNullValues() {
            // Given
            SignUpRequest request = new SignUpRequest();

            // When
            request.setFirstName(null);
            request.setLastName(null);
            request.setEmail(null);
            request.setPassword(null);
            request.setConfirmPassword(null);
            request.setAddress(null);
            request.setBirthDate(null);
            request.setRole(null);
            request.setFaceDescriptor(null);

            // Then
            assertNull(request.getFirstName());
            assertNull(request.getLastName());
            assertNull(request.getEmail());
            assertNull(request.getPassword());
            assertNull(request.getConfirmPassword());
            assertNull(request.getAddress());
            assertNull(request.getBirthDate());
            assertNull(request.getRole());
            assertNull(request.getFaceDescriptor());
        }
    }

    @Nested
    @DisplayName("SignInRequest Tests")
    class SignInRequestTests {

        @Test
        @DisplayName("Should create SignInRequest with email and password")
        void shouldCreateSignInRequestWithEmailAndPassword() {
            // Given
            SignInRequest request = new SignInRequest();

            // When
            request.setEmail("john.doe@example.com");
            request.setPassword("password123");

            // Then
            assertEquals("john.doe@example.com", request.getEmail());
            assertEquals("password123", request.getPassword());
        }

        @Test
        @DisplayName("Should handle empty values")
        void shouldHandleEmptyValues() {
            // Given
            SignInRequest request = new SignInRequest();

            // When
            request.setEmail("");
            request.setPassword("");

            // Then
            assertEquals("", request.getEmail());
            assertEquals("", request.getPassword());
        }
    }

    @Nested
    @DisplayName("FaceSignInRequest Tests")
    class FaceSignInRequestTests {

        @Test
        @DisplayName("Should create FaceSignInRequest with email and descriptor")
        void shouldCreateFaceSignInRequestWithEmailAndDescriptor() {
            // Given
            FaceSignInRequest request = new FaceSignInRequest();

            // When
            request.setEmail("john.doe@example.com");
            request.setFaceDescriptor("faceData123");

            // Then
            assertEquals("john.doe@example.com", request.getEmail());
            assertEquals("faceData123", request.getFaceDescriptor());
        }
    }

    @Nested
    @DisplayName("PasswordResetRequest Tests")
    class PasswordResetRequestTests {

        @Test
        @DisplayName("Should create PasswordResetRequest with email")
        void shouldCreatePasswordResetRequestWithEmail() {
            // Given
            PasswordResetRequest request = new PasswordResetRequest();

            // When
            request.setEmail("john.doe@example.com");

            // Then
            assertEquals("john.doe@example.com", request.getEmail());
        }
    }

    @Nested
    @DisplayName("PasswordResetConfirm Tests")
    class PasswordResetConfirmTests {

        @Test
        @DisplayName("Should create PasswordResetConfirm with all fields")
        void shouldCreatePasswordResetConfirmWithAllFields() {
            // Given
            PasswordResetConfirm request = new PasswordResetConfirm();

            // When
            request.setToken("resetToken123");
            request.setNewPassword("newPassword123");
            request.setConfirmPassword("newPassword123");

            // Then
            assertEquals("resetToken123", request.getToken());
            assertEquals("newPassword123", request.getNewPassword());
            assertEquals("newPassword123", request.getConfirmPassword());
        }
    }

    @Nested
    @DisplayName("EmailVerificationRequest Tests")
    class EmailVerificationRequestTests {

        @Test
        @DisplayName("Should create EmailVerificationRequest with email and code")
        void shouldCreateEmailVerificationRequestWithEmailAndCode() {
            // Given
            EmailVerificationRequest request = new EmailVerificationRequest();

            // When
            request.setEmail("john.doe@example.com");
            request.setVerificationCode("123456");

            // Then
            assertEquals("john.doe@example.com", request.getEmail());
            assertEquals("123456", request.getVerificationCode());
        }
    }

    @Nested
    @DisplayName("ResendVerificationRequest Tests")
    class ResendVerificationRequestTests {

        @Test
        @DisplayName("Should create ResendVerificationRequest with email")
        void shouldCreateResendVerificationRequestWithEmail() {
            // Given
            ResendVerificationRequest request = new ResendVerificationRequest();

            // When
            request.setEmail("john.doe@example.com");

            // Then
            assertEquals("john.doe@example.com", request.getEmail());
        }
    }

    @Nested
    @DisplayName("PasswordChangeRequest Tests")
    class PasswordChangeRequestTests {

        @Test
        @DisplayName("Should create PasswordChangeRequest with all fields")
        void shouldCreatePasswordChangeRequestWithAllFields() {
            // Given
            PasswordChangeRequest request = new PasswordChangeRequest();

            // When
            request.setEmail("john.doe@example.com");
            request.setVerificationCode("123456");
            request.setOldPassword("oldPassword");
            request.setNewPassword("newPassword");

            // Then
            assertEquals("john.doe@example.com", request.getEmail());
            assertEquals("123456", request.getVerificationCode());
            assertEquals("oldPassword", request.getOldPassword());
            assertEquals("newPassword", request.getNewPassword());
        }
    }

    @Nested
    @DisplayName("JwtResponse Tests")
    class JwtResponseTests {

        @Test
        @DisplayName("Should create JwtResponse with all fields")
        void shouldCreateJwtResponseWithAllFields() {
            // Given
            JwtResponse response = new JwtResponse();

            // When
            response.setToken("jwtToken123");
            response.setType("Bearer");
            response.setId(1L);
            response.setEmail("john.doe@example.com");
            response.setFirstName("John");
            response.setLastName("Doe");
            response.setRole(Role.FREELANCER);

            // Then
            assertEquals("jwtToken123", response.getToken());
            assertEquals("Bearer", response.getType());
            assertEquals(1L, response.getId());
            assertEquals("john.doe@example.com", response.getEmail());
            assertEquals("John", response.getFirstName());
            assertEquals("Doe", response.getLastName());
            assertEquals(Role.FREELANCER, response.getRole());
        }
    }

    @Nested
    @DisplayName("UserStatsResponse Tests")
    class UserStatsResponseTests {

        @Test
        @DisplayName("Should create UserStatsResponse with all fields")
        void shouldCreateUserStatsResponseWithAllFields() {
            // Given & When
            UserStatsResponse response = new UserStatsResponse(
                    100L, 30L, 25L, 80L, 20L, 5L
            );

            // Then
            assertEquals(100L, response.getTotalUsers());
            assertEquals(30L, response.getActiveFreelancers());
            assertEquals(25L, response.getActiveProjectOwners());
            assertEquals(80L, response.getActiveAccounts());
            assertEquals(20L, response.getInactiveAccounts());
            assertEquals(5L, response.getTotalAdmins());
        }

        @Test
        @DisplayName("Should create UserStatsResponse with zero values")
        void shouldCreateUserStatsResponseWithZeroValues() {
            // Given & When
            UserStatsResponse response = new UserStatsResponse(
                    0L, 0L, 0L, 0L, 0L, 0L
            );

            // Then
            assertEquals(0L, response.getTotalUsers());
            assertEquals(0L, response.getActiveFreelancers());
            assertEquals(0L, response.getActiveProjectOwners());
            assertEquals(0L, response.getActiveAccounts());
            assertEquals(0L, response.getInactiveAccounts());
            assertEquals(0L, response.getTotalAdmins());
        }
    }

    @Nested
    @DisplayName("UserFilterRequest Tests")
    class UserFilterRequestTests {

        @Test
        @DisplayName("Should create UserFilterRequest with all fields")
        void shouldCreateUserFilterRequestWithAllFields() {
            // Given
            UserFilterRequest request = new UserFilterRequest();

            // When
            request.setName("John");
            request.setEmail("john@example.com");
            request.setRole(Role.FREELANCER);
            request.setEnabled(true);
            request.setPage(0);
            request.setSize(10);
            request.setSortBy("firstName");
            request.setSortDir("ASC");

            // Then
            assertEquals("John", request.getName());
            assertEquals("john@example.com", request.getEmail());
            assertEquals(Role.FREELANCER, request.getRole());
            assertTrue(request.getEnabled());
            assertEquals(0, request.getPage());
            assertEquals(10, request.getSize());
            assertEquals("firstName", request.getSortBy());
            assertEquals("ASC", request.getSortDir());
        }

        @Test
        @DisplayName("Should handle default values")
        void shouldHandleDefaultValues() {
            // Given
            UserFilterRequest request = new UserFilterRequest();

            // Then
            assertNull(request.getName());
            assertNull(request.getEmail());
            assertNull(request.getRole());
            assertNull(request.getEnabled());
            assertEquals(0, request.getPage());
            assertEquals(10, request.getSize());
            assertEquals("firstName", request.getSortBy());
            assertEquals("ASC", request.getSortDir());
        }
    }

    @Nested
    @DisplayName("PageResponse Tests")
    class PageResponseTests {

        @Test
        @DisplayName("Should create PageResponse with all fields")
        void shouldCreatePageResponseWithAllFields() {
            // Given
            PageResponse<String> response = new PageResponse<>();

            // When
            response.setContent(java.util.Arrays.asList("item1", "item2"));
            response.setTotalElements(100L);
            response.setTotalPages(10);
            response.setSize(10);
            response.setNumber(0);
            response.setFirst(true);
            response.setLast(false);

            // Then
            assertEquals(2, response.getContent().size());
            assertEquals("item1", response.getContent().get(0));
            assertEquals("item2", response.getContent().get(1));
            assertEquals(100L, response.getTotalElements());
            assertEquals(10, response.getTotalPages());
            assertEquals(10, response.getSize());
            assertEquals(0, response.getNumber());
            assertTrue(response.isFirst());
            assertFalse(response.isLast());
        }

        @Test
        @DisplayName("Should handle empty content")
        void shouldHandleEmptyContent() {
            // Given
            PageResponse<String> response = new PageResponse<>();

            // When
            response.setContent(java.util.Collections.emptyList());
            response.setTotalElements(0L);
            response.setTotalPages(0);
            response.setSize(10);
            response.setNumber(0);
            response.setFirst(true);
            response.setLast(true);

            // Then
            assertTrue(response.getContent().isEmpty());
            assertEquals(0L, response.getTotalElements());
            assertEquals(0, response.getTotalPages());
            assertTrue(response.isFirst());
            assertTrue(response.isLast());
        }
    }

    @Nested
    @DisplayName("Edge Cases Tests")
    class EdgeCasesTests {

        @Test
        @DisplayName("Should handle special characters in string fields")
        void shouldHandleSpecialCharactersInStringFields() {
            // Given
            SignUpRequest request = new SignUpRequest();
            String specialChars = "!@#$%^&*()_+-=[]{}|;':\",./<>?`~";

            // When
            request.setFirstName(specialChars);
            request.setLastName(specialChars);
            request.setEmail(specialChars);
            request.setPassword(specialChars);
            request.setAddress(specialChars);

            // Then
            assertEquals(specialChars, request.getFirstName());
            assertEquals(specialChars, request.getLastName());
            assertEquals(specialChars, request.getEmail());
            assertEquals(specialChars, request.getPassword());
            assertEquals(specialChars, request.getAddress());
        }

        @Test
        @DisplayName("Should handle very long strings")
        void shouldHandleVeryLongStrings() {
            // Given
            SignUpRequest request = new SignUpRequest();
            String longString = "a".repeat(10000);

            // When
            request.setFirstName(longString);
            request.setLastName(longString);
            request.setEmail(longString);
            request.setPassword(longString);
            request.setAddress(longString);

            // Then
            assertEquals(longString, request.getFirstName());
            assertEquals(longString, request.getLastName());
            assertEquals(longString, request.getEmail());
            assertEquals(longString, request.getPassword());
            assertEquals(longString, request.getAddress());
        }

        @Test
        @DisplayName("Should handle unicode characters")
        void shouldHandleUnicodeCharacters() {
            // Given
            SignUpRequest request = new SignUpRequest();
            String unicode = "Jöhn Döé 测试 🚀 émojis";

            // When
            request.setFirstName(unicode);
            request.setLastName(unicode);
            request.setEmail(unicode);

            // Then
            assertEquals(unicode, request.getFirstName());
            assertEquals(unicode, request.getLastName());
            assertEquals(unicode, request.getEmail());
        }

        @Test
        @DisplayName("Should handle negative values in numeric fields")
        void shouldHandleNegativeValuesInNumericFields() {
            // Given
            UserFilterRequest request = new UserFilterRequest();

            // When
            request.setPage(-1);
            request.setSize(-10);

            // Then
            assertEquals(-1, request.getPage());
            assertEquals(-10, request.getSize());
        }

        @Test
        @DisplayName("Should handle very large numeric values")
        void shouldHandleVeryLargeNumericValues() {
            // Given
            UserStatsResponse response = new UserStatsResponse(
                    Long.MAX_VALUE,
                    Long.MAX_VALUE,
                    Long.MAX_VALUE,
                    Long.MAX_VALUE,
                    Long.MAX_VALUE,
                    Long.MAX_VALUE
            );

            // Then
            assertEquals(Long.MAX_VALUE, response.getTotalUsers());
            assertEquals(Long.MAX_VALUE, response.getActiveFreelancers());
            assertEquals(Long.MAX_VALUE, response.getActiveProjectOwners());
            assertEquals(Long.MAX_VALUE, response.getActiveAccounts());
            assertEquals(Long.MAX_VALUE, response.getInactiveAccounts());
            assertEquals(Long.MAX_VALUE, response.getTotalAdmins());
        }
    }
}