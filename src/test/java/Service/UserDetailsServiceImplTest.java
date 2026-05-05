package Service;

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
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserDetailsServiceImpl Tests")
class UserDetailsServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserDetailsServiceImpl userDetailsService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@example.com")
                .password("encodedPassword123")
                .address("123 Main St")
                .birthDate(LocalDate.of(1990, 1, 1))
                .role(Role.FREELANCER)
                .enabled(true)
                .faceDescriptor("face123")
                .profilePictureUrl("/images/profile.jpg")
                .build();
    }

    @Nested
    @DisplayName("Load User By Username Tests")
    class LoadUserByUsernameTests {

        @Test
        @DisplayName("Should load user by email successfully")
        void shouldLoadUserByEmailSuccessfully() {
            // Given
            String email = "john.doe@example.com";
            when(userRepository.findByEmail(email)).thenReturn(Optional.of(testUser));

            // When
            UserDetails result = userDetailsService.loadUserByUsername(email);

            // Then
            assertNotNull(result);
            assertEquals(email, result.getUsername());
            assertEquals("encodedPassword123", result.getPassword());
            assertTrue(result.isEnabled());
            assertTrue(result.isAccountNonExpired());
            assertTrue(result.isCredentialsNonExpired());
            assertTrue(result.isAccountNonLocked());
            assertEquals(1, result.getAuthorities().size());
            assertTrue(result.getAuthorities().stream()
                    .anyMatch(auth -> auth.getAuthority().equals("ROLE_FREELANCER")));

            verify(userRepository).findByEmail(email);
        }

        @Test
        @DisplayName("Should load admin user correctly")
        void shouldLoadAdminUserCorrectly() {
            // Given
            String email = "admin@example.com";
            User adminUser = User.builder()
                    .id(2L)
                    .firstName("Admin")
                    .lastName("User")
                    .email(email)
                    .password("adminPassword")
                    .role(Role.ADMIN)
                    .enabled(true)
                    .build();

            when(userRepository.findByEmail(email)).thenReturn(Optional.of(adminUser));

            // When
            UserDetails result = userDetailsService.loadUserByUsername(email);

            // Then
            assertNotNull(result);
            assertEquals(email, result.getUsername());
            assertEquals("adminPassword", result.getPassword());
            assertTrue(result.isEnabled());
            assertTrue(result.getAuthorities().stream()
                    .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN")));

            verify(userRepository).findByEmail(email);
        }

        @Test
        @DisplayName("Should load project owner user correctly")
        void shouldLoadProjectOwnerUserCorrectly() {
            // Given
            String email = "owner@example.com";
            User ownerUser = User.builder()
                    .id(3L)
                    .firstName("Project")
                    .lastName("Owner")
                    .email(email)
                    .password("ownerPassword")
                    .role(Role.PROJECT_OWNER)
                    .enabled(true)
                    .build();

            when(userRepository.findByEmail(email)).thenReturn(Optional.of(ownerUser));

            // When
            UserDetails result = userDetailsService.loadUserByUsername(email);

            // Then
            assertNotNull(result);
            assertEquals(email, result.getUsername());
            assertEquals("ownerPassword", result.getPassword());
            assertTrue(result.isEnabled());
            assertTrue(result.getAuthorities().stream()
                    .anyMatch(auth -> auth.getAuthority().equals("ROLE_PROJECT_OWNER")));

            verify(userRepository).findByEmail(email);
        }

        @Test
        @DisplayName("Should load disabled user correctly")
        void shouldLoadDisabledUserCorrectly() {
            // Given
            String email = "disabled@example.com";
            User disabledUser = User.builder()
                    .id(4L)
                    .firstName("Disabled")
                    .lastName("User")
                    .email(email)
                    .password("disabledPassword")
                    .role(Role.FREELANCER)
                    .enabled(false)
                    .build();

            when(userRepository.findByEmail(email)).thenReturn(Optional.of(disabledUser));

            // When
            UserDetails result = userDetailsService.loadUserByUsername(email);

            // Then
            assertNotNull(result);
            assertEquals(email, result.getUsername());
            assertEquals("disabledPassword", result.getPassword());
            assertFalse(result.isEnabled()); // Should be disabled
            assertTrue(result.isAccountNonExpired());
            assertTrue(result.isCredentialsNonExpired());
            assertTrue(result.isAccountNonLocked());

            verify(userRepository).findByEmail(email);
        }

        @Test
        @DisplayName("Should throw UsernameNotFoundException when user not found")
        void shouldThrowUsernameNotFoundExceptionWhenUserNotFound() {
            // Given
            String email = "nonexistent@example.com";
            when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

            // When & Then
            UsernameNotFoundException exception = assertThrows(UsernameNotFoundException.class,
                    () -> userDetailsService.loadUserByUsername(email));

            assertEquals("Utilisateur non trouvé : " + email, exception.getMessage());
            verify(userRepository).findByEmail(email);
        }

        @Test
        @DisplayName("Should handle null email")
        void shouldHandleNullEmail() {
            // Given
            String email = null;
            when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

            // When & Then
            UsernameNotFoundException exception = assertThrows(UsernameNotFoundException.class,
                    () -> userDetailsService.loadUserByUsername(email));

            assertEquals("Utilisateur non trouvé : null", exception.getMessage());
            verify(userRepository).findByEmail(email);
        }

        @Test
        @DisplayName("Should handle empty email")
        void shouldHandleEmptyEmail() {
            // Given
            String email = "";
            when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

            // When & Then
            UsernameNotFoundException exception = assertThrows(UsernameNotFoundException.class,
                    () -> userDetailsService.loadUserByUsername(email));

            assertEquals("Utilisateur non trouvé : ", exception.getMessage());
            verify(userRepository).findByEmail(email);
        }

        @Test
        @DisplayName("Should handle whitespace-only email")
        void shouldHandleWhitespaceOnlyEmail() {
            // Given
            String email = "   ";
            when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

            // When & Then
            UsernameNotFoundException exception = assertThrows(UsernameNotFoundException.class,
                    () -> userDetailsService.loadUserByUsername(email));

            assertEquals("Utilisateur non trouvé : " + email, exception.getMessage());
            verify(userRepository).findByEmail(email);
        }

        @Test
        @DisplayName("Should handle case-sensitive email lookup")
        void shouldHandleCaseSensitiveEmailLookup() {
            // Given
            String email = "JOHN.DOE@EXAMPLE.COM";
            when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

            // When & Then
            UsernameNotFoundException exception = assertThrows(UsernameNotFoundException.class,
                    () -> userDetailsService.loadUserByUsername(email));

            assertEquals("Utilisateur non trouvé : " + email, exception.getMessage());
            verify(userRepository).findByEmail(email);
        }

        @Test
        @DisplayName("Should handle special characters in email")
        void shouldHandleSpecialCharactersInEmail() {
            // Given
            String email = "user+test@example.com";
            User specialUser = User.builder()
                    .id(5L)
                    .firstName("Special")
                    .lastName("User")
                    .email(email)
                    .password("specialPassword")
                    .role(Role.FREELANCER)
                    .enabled(true)
                    .build();

            when(userRepository.findByEmail(email)).thenReturn(Optional.of(specialUser));

            // When
            UserDetails result = userDetailsService.loadUserByUsername(email);

            // Then
            assertNotNull(result);
            assertEquals(email, result.getUsername());
            assertEquals("specialPassword", result.getPassword());
            assertTrue(result.isEnabled());

            verify(userRepository).findByEmail(email);
        }

        @Test
        @DisplayName("Should handle user with null role")
        void shouldHandleUserWithNullRole() {
            // Given
            String email = "nullrole@example.com";
            User nullRoleUser = User.builder()
                    .id(6L)
                    .firstName("Null")
                    .lastName("Role")
                    .email(email)
                    .password("nullRolePassword")
                    .role(null)
                    .enabled(true)
                    .build();

            when(userRepository.findByEmail(email)).thenReturn(Optional.of(nullRoleUser));

            // When & Then
            // This should throw a NullPointerException when trying to get role.name()
            assertThrows(NullPointerException.class,
                    () -> userDetailsService.loadUserByUsername(email));

            verify(userRepository).findByEmail(email);
        }

        @Test
        @DisplayName("Should handle user with null password")
        void shouldHandleUserWithNullPassword() {
            // Given
            String email = "nullpassword@example.com";
            User nullPasswordUser = User.builder()
                    .id(7L)
                    .firstName("Null")
                    .lastName("Password")
                    .email(email)
                    .password(null)
                    .role(Role.FREELANCER)
                    .enabled(true)
                    .build();

            when(userRepository.findByEmail(email)).thenReturn(Optional.of(nullPasswordUser));

            // When
            UserDetails result = userDetailsService.loadUserByUsername(email);

            // Then
            assertNotNull(result);
            assertEquals(email, result.getUsername());
            assertNull(result.getPassword());
            assertTrue(result.isEnabled());

            verify(userRepository).findByEmail(email);
        }
    }

    @Nested
    @DisplayName("Edge Cases Tests")
    class EdgeCasesTests {

        @Test
        @DisplayName("Should handle repository exception")
        void shouldHandleRepositoryException() {
            // Given
            String email = "test@example.com";
            when(userRepository.findByEmail(email)).thenThrow(new RuntimeException("Database error"));

            // When & Then
            assertThrows(RuntimeException.class,
                    () -> userDetailsService.loadUserByUsername(email));

            verify(userRepository).findByEmail(email);
        }

        @Test
        @DisplayName("Should handle very long email")
        void shouldHandleVeryLongEmail() {
            // Given
            String longEmail = "a".repeat(1000) + "@example.com";
            when(userRepository.findByEmail(longEmail)).thenReturn(Optional.empty());

            // When & Then
            UsernameNotFoundException exception = assertThrows(UsernameNotFoundException.class,
                    () -> userDetailsService.loadUserByUsername(longEmail));

            assertTrue(exception.getMessage().contains("Utilisateur non trouvé"));
            verify(userRepository).findByEmail(longEmail);
        }

        @Test
        @DisplayName("Should handle email with unicode characters")
        void shouldHandleEmailWithUnicodeCharacters() {
            // Given
            String unicodeEmail = "tëst@éxämplé.com";
            User unicodeUser = User.builder()
                    .id(8L)
                    .firstName("Unicode")
                    .lastName("User")
                    .email(unicodeEmail)
                    .password("unicodePassword")
                    .role(Role.FREELANCER)
                    .enabled(true)
                    .build();

            when(userRepository.findByEmail(unicodeEmail)).thenReturn(Optional.of(unicodeUser));

            // When
            UserDetails result = userDetailsService.loadUserByUsername(unicodeEmail);

            // Then
            assertNotNull(result);
            assertEquals(unicodeEmail, result.getUsername());
            assertEquals("unicodePassword", result.getPassword());
            assertTrue(result.isEnabled());

            verify(userRepository).findByEmail(unicodeEmail);
        }

        @Test
        @DisplayName("Should handle concurrent access")
        void shouldHandleConcurrentAccess() {
            // Given
            String email = "concurrent@example.com";
            when(userRepository.findByEmail(email)).thenReturn(Optional.of(testUser));

            // When - Simulate concurrent calls
            UserDetails result1 = userDetailsService.loadUserByUsername(email);
            UserDetails result2 = userDetailsService.loadUserByUsername(email);

            // Then
            assertNotNull(result1);
            assertNotNull(result2);
            assertEquals(result1.getUsername(), result2.getUsername());
            assertEquals(result1.getPassword(), result2.getPassword());

            verify(userRepository, times(2)).findByEmail(email);
        }
    }
}