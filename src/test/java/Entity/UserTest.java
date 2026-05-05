package Entity;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import jakarta.persistence.*;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("User Entity Tests")
class UserTest {

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {

        @Test
        @DisplayName("Should create user with default constructor")
        void shouldCreateUserWithDefaultConstructor() {
            // Given & When
            User user = new User();

            // Then
            assertNotNull(user);
            assertNull(user.getId());
            assertNull(user.getFirstName());
            assertNull(user.getLastName());
            assertNull(user.getEmail());
            assertNull(user.getPassword());
            assertNull(user.getAddress());
            assertNull(user.getBirthDate());
            assertNull(user.getRole());
            assertNull(user.getFaceDescriptor());
            assertNull(user.getProfilePictureUrl());
            assertTrue(user.isEnabled()); // Default value
        }

        @Test
        @DisplayName("Should create user with parameterized constructor")
        void shouldCreateUserWithParameterizedConstructor() {
            // Given
            Long id = 1L;
            String firstName = "John";
            String lastName = "Doe";
            String address = "123 Main St";
            String email = "john.doe@example.com";
            String password = "password123";
            LocalDate birthDate = LocalDate.of(1990, 1, 1);
            Role role = Role.FREELANCER;
            boolean enabled = true;
            String faceDescriptor = "face123";
            String profilePictureUrl = "/images/profile.jpg";

            // When
            User user = new User(id, firstName, lastName, address, email, password, 
                               birthDate, role, enabled, faceDescriptor, profilePictureUrl);

            // Then
            assertEquals(id, user.getId());
            assertEquals(firstName, user.getFirstName());
            assertEquals(lastName, user.getLastName());
            assertEquals(address, user.getAddress());
            assertEquals(email, user.getEmail());
            assertEquals(password, user.getPassword());
            assertEquals(birthDate, user.getBirthDate());
            assertEquals(role, user.getRole());
            assertEquals(enabled, user.isEnabled());
            assertEquals(faceDescriptor, user.getFaceDescriptor());
            assertEquals(profilePictureUrl, user.getProfilePictureUrl());
        }
    }

    @Nested
    @DisplayName("Builder Pattern Tests")
    class BuilderTests {

        @Test
        @DisplayName("Should create user with builder pattern")
        void shouldCreateUserWithBuilder() {
            // Given & When
            User user = User.builder()
                    .firstName("Jane")
                    .lastName("Smith")
                    .address("456 Oak Ave")
                    .email("jane.smith@example.com")
                    .password("securePassword")
                    .birthDate(LocalDate.of(1985, 5, 15))
                    .role(Role.PROJECT_OWNER)
                    .faceDescriptor("faceData456")
                    .profilePictureUrl("/images/jane.jpg")
                    .enabled(false)
                    .build();

            // Then
            assertEquals("Jane", user.getFirstName());
            assertEquals("Smith", user.getLastName());
            assertEquals("456 Oak Ave", user.getAddress());
            assertEquals("jane.smith@example.com", user.getEmail());
            assertEquals("securePassword", user.getPassword());
            assertEquals(LocalDate.of(1985, 5, 15), user.getBirthDate());
            assertEquals(Role.PROJECT_OWNER, user.getRole());
            assertEquals("faceData456", user.getFaceDescriptor());
            assertEquals("/images/jane.jpg", user.getProfilePictureUrl());
            assertFalse(user.isEnabled());
        }

        @Test
        @DisplayName("Should create user with minimal builder data")
        void shouldCreateUserWithMinimalBuilderData() {
            // Given & When
            User user = User.builder()
                    .firstName("Bob")
                    .email("bob@example.com")
                    .build();

            // Then
            assertEquals("Bob", user.getFirstName());
            assertEquals("bob@example.com", user.getEmail());
            assertNull(user.getLastName());
            assertNull(user.getAddress());
            assertNull(user.getPassword());
            assertNull(user.getBirthDate());
            assertNull(user.getRole());
            assertNull(user.getFaceDescriptor());
            assertNull(user.getProfilePictureUrl());
            assertTrue(user.isEnabled()); // Default value
        }

        @Test
        @DisplayName("Should create builder instance")
        void shouldCreateBuilderInstance() {
            // Given & When
            User.UserBuilder builder = User.builder();

            // Then
            assertNotNull(builder);
        }
    }

    @Nested
    @DisplayName("Getters and Setters Tests")
    class GettersSettersTests {

        @Test
        @DisplayName("Should set and get all properties correctly")
        void shouldSetAndGetAllProperties() {
            // Given
            User user = new User();
            Long id = 100L;
            String firstName = "Alice";
            String lastName = "Johnson";
            String address = "789 Pine St";
            String email = "alice.johnson@example.com";
            String password = "myPassword";
            LocalDate birthDate = LocalDate.of(1992, 12, 25);
            Role role = Role.ADMIN;
            String faceDescriptor = "aliceFaceData";
            String profilePictureUrl = "/images/alice.png";
            boolean enabled = false;

            // When
            user.setId(id);
            user.setFirstName(firstName);
            user.setLastName(lastName);
            user.setAddress(address);
            user.setEmail(email);
            user.setPassword(password);
            user.setBirthDate(birthDate);
            user.setRole(role);
            user.setFaceDescriptor(faceDescriptor);
            user.setProfilePictureUrl(profilePictureUrl);
            user.setEnabled(enabled);

            // Then
            assertEquals(id, user.getId());
            assertEquals(firstName, user.getFirstName());
            assertEquals(lastName, user.getLastName());
            assertEquals(address, user.getAddress());
            assertEquals(email, user.getEmail());
            assertEquals(password, user.getPassword());
            assertEquals(birthDate, user.getBirthDate());
            assertEquals(role, user.getRole());
            assertEquals(faceDescriptor, user.getFaceDescriptor());
            assertEquals(profilePictureUrl, user.getProfilePictureUrl());
            assertEquals(enabled, user.isEnabled());
        }

        @Test
        @DisplayName("Should handle null values correctly")
        void shouldHandleNullValues() {
            // Given
            User user = new User();

            // When
            user.setId(null);
            user.setFirstName(null);
            user.setLastName(null);
            user.setAddress(null);
            user.setEmail(null);
            user.setPassword(null);
            user.setBirthDate(null);
            user.setRole(null);
            user.setFaceDescriptor(null);
            user.setProfilePictureUrl(null);

            // Then
            assertNull(user.getId());
            assertNull(user.getFirstName());
            assertNull(user.getLastName());
            assertNull(user.getAddress());
            assertNull(user.getEmail());
            assertNull(user.getPassword());
            assertNull(user.getBirthDate());
            assertNull(user.getRole());
            assertNull(user.getFaceDescriptor());
            assertNull(user.getProfilePictureUrl());
        }
    }

    @Nested
    @DisplayName("Role Enum Tests")
    class RoleTests {

        @Test
        @DisplayName("Should have all expected role values")
        void shouldHaveAllExpectedRoleValues() {
            // Given & When
            Role[] roles = Role.values();

            // Then
            assertEquals(3, roles.length);
            assertTrue(java.util.Arrays.asList(roles).contains(Role.ADMIN));
            assertTrue(java.util.Arrays.asList(roles).contains(Role.PROJECT_OWNER));
            assertTrue(java.util.Arrays.asList(roles).contains(Role.FREELANCER));
        }

        @Test
        @DisplayName("Should convert role to string correctly")
        void shouldConvertRoleToString() {
            // Given & When & Then
            assertEquals("ADMIN", Role.ADMIN.toString());
            assertEquals("PROJECT_OWNER", Role.PROJECT_OWNER.toString());
            assertEquals("FREELANCER", Role.FREELANCER.toString());
        }

        @Test
        @DisplayName("Should parse role from string correctly")
        void shouldParseRoleFromString() {
            // Given & When & Then
            assertEquals(Role.ADMIN, Role.valueOf("ADMIN"));
            assertEquals(Role.PROJECT_OWNER, Role.valueOf("PROJECT_OWNER"));
            assertEquals(Role.FREELANCER, Role.valueOf("FREELANCER"));
        }
    }

    @Nested
    @DisplayName("Edge Cases Tests")
    class EdgeCasesTests {

        @Test
        @DisplayName("Should handle empty strings")
        void shouldHandleEmptyStrings() {
            // Given
            User user = new User();

            // When
            user.setFirstName("");
            user.setLastName("");
            user.setAddress("");
            user.setEmail("");
            user.setPassword("");
            user.setFaceDescriptor("");
            user.setProfilePictureUrl("");

            // Then
            assertEquals("", user.getFirstName());
            assertEquals("", user.getLastName());
            assertEquals("", user.getAddress());
            assertEquals("", user.getEmail());
            assertEquals("", user.getPassword());
            assertEquals("", user.getFaceDescriptor());
            assertEquals("", user.getProfilePictureUrl());
        }

        @Test
        @DisplayName("Should handle very long strings")
        void shouldHandleVeryLongStrings() {
            // Given
            User user = new User();
            String longString = "a".repeat(1000);

            // When
            user.setFirstName(longString);
            user.setLastName(longString);
            user.setAddress(longString);
            user.setEmail(longString);
            user.setPassword(longString);
            user.setFaceDescriptor(longString);
            user.setProfilePictureUrl(longString);

            // Then
            assertEquals(longString, user.getFirstName());
            assertEquals(longString, user.getLastName());
            assertEquals(longString, user.getAddress());
            assertEquals(longString, user.getEmail());
            assertEquals(longString, user.getPassword());
            assertEquals(longString, user.getFaceDescriptor());
            assertEquals(longString, user.getProfilePictureUrl());
        }

        @Test
        @DisplayName("Should handle special characters in strings")
        void shouldHandleSpecialCharacters() {
            // Given
            User user = new User();
            String specialChars = "!@#$%^&*()_+-=[]{}|;':\",./<>?`~";

            // When
            user.setFirstName(specialChars);
            user.setLastName(specialChars);
            user.setAddress(specialChars);
            user.setEmail(specialChars);
            user.setPassword(specialChars);
            user.setFaceDescriptor(specialChars);
            user.setProfilePictureUrl(specialChars);

            // Then
            assertEquals(specialChars, user.getFirstName());
            assertEquals(specialChars, user.getLastName());
            assertEquals(specialChars, user.getAddress());
            assertEquals(specialChars, user.getEmail());
            assertEquals(specialChars, user.getPassword());
            assertEquals(specialChars, user.getFaceDescriptor());
            assertEquals(specialChars, user.getProfilePictureUrl());
        }

        @Test
        @DisplayName("Should handle future birth dates")
        void shouldHandleFutureBirthDates() {
            // Given
            User user = new User();
            LocalDate futureDate = LocalDate.now().plusYears(10);

            // When
            user.setBirthDate(futureDate);

            // Then
            assertEquals(futureDate, user.getBirthDate());
        }

        @Test
        @DisplayName("Should handle very old birth dates")
        void shouldHandleVeryOldBirthDates() {
            // Given
            User user = new User();
            LocalDate oldDate = LocalDate.of(1800, 1, 1);

            // When
            user.setBirthDate(oldDate);

            // Then
            assertEquals(oldDate, user.getBirthDate());
        }
    }

    @Nested
    @DisplayName("JPA Annotation Tests")
    class JpaAnnotationTests {

        @Test
        @DisplayName("Should have correct entity annotation")
        void shouldHaveCorrectEntityAnnotation() {
            // Given & When
            Class<User> userClass = User.class;

            // Then
            assertTrue(userClass.isAnnotationPresent(Entity.class));
            assertTrue(userClass.isAnnotationPresent(Table.class));
            
            Table tableAnnotation = userClass.getAnnotation(Table.class);
            assertEquals("users", tableAnnotation.name());
        }

        @Test
        @DisplayName("Should have correct field annotations")
        void shouldHaveCorrectFieldAnnotations() throws NoSuchFieldException {
            // Given
            Class<User> userClass = User.class;

            // Then - Check ID field
            var idField = userClass.getDeclaredField("id");
            assertTrue(idField.isAnnotationPresent(Id.class));
            assertTrue(idField.isAnnotationPresent(GeneratedValue.class));
            
            GeneratedValue generatedValue = idField.getAnnotation(GeneratedValue.class);
            assertEquals(GenerationType.IDENTITY, generatedValue.strategy());

            // Check required fields have nullable = false
            var firstNameField = userClass.getDeclaredField("firstName");
            assertTrue(firstNameField.isAnnotationPresent(Column.class));
            assertFalse(firstNameField.getAnnotation(Column.class).nullable());

            var emailField = userClass.getDeclaredField("email");
            assertTrue(emailField.isAnnotationPresent(Column.class));
            Column emailColumn = emailField.getAnnotation(Column.class);
            assertFalse(emailColumn.nullable());
            assertTrue(emailColumn.unique());

            // Check role field enumeration
            var roleField = userClass.getDeclaredField("role");
            assertTrue(roleField.isAnnotationPresent(Enumerated.class));
            assertEquals(EnumType.STRING, roleField.getAnnotation(Enumerated.class).value());

            // Check face descriptor TEXT column
            var faceDescriptorField = userClass.getDeclaredField("faceDescriptor");
            assertTrue(faceDescriptorField.isAnnotationPresent(Column.class));
            assertEquals("TEXT", faceDescriptorField.getAnnotation(Column.class).columnDefinition());

            // Check profile picture URL length
            var profilePictureField = userClass.getDeclaredField("profilePictureUrl");
            assertTrue(profilePictureField.isAnnotationPresent(Column.class));
            assertEquals(255, profilePictureField.getAnnotation(Column.class).length());
        }
    }

    @Nested
    @DisplayName("Business Logic Tests")
    class BusinessLogicTests {

        @Test
        @DisplayName("Should have enabled as true by default")
        void shouldHaveEnabledAsTrueByDefault() {
            // Given & When
            User user = new User();

            // Then
            assertTrue(user.isEnabled());
        }

        @Test
        @DisplayName("Should maintain enabled state through builder")
        void shouldMaintainEnabledStateThroughBuilder() {
            // Given & When
            User enabledUser = User.builder()
                    .firstName("John")
                    .email("john@example.com")
                    .enabled(true)
                    .build();

            User disabledUser = User.builder()
                    .firstName("Jane")
                    .email("jane@example.com")
                    .enabled(false)
                    .build();

            // Then
            assertTrue(enabledUser.isEnabled());
            assertFalse(disabledUser.isEnabled());
        }

        @Test
        @DisplayName("Should handle role transitions correctly")
        void shouldHandleRoleTransitionsCorrectly() {
            // Given
            User user = new User();

            // When & Then
            user.setRole(Role.FREELANCER);
            assertEquals(Role.FREELANCER, user.getRole());

            user.setRole(Role.PROJECT_OWNER);
            assertEquals(Role.PROJECT_OWNER, user.getRole());

            user.setRole(Role.ADMIN);
            assertEquals(Role.ADMIN, user.getRole());
        }

        @Test
        @DisplayName("Should handle profile picture URL updates")
        void shouldHandleProfilePictureUrlUpdates() {
            // Given
            User user = new User();
            String initialUrl = "/images/default.jpg";
            String updatedUrl = "/images/updated.png";

            // When
            user.setProfilePictureUrl(initialUrl);
            assertEquals(initialUrl, user.getProfilePictureUrl());

            user.setProfilePictureUrl(updatedUrl);

            // Then
            assertEquals(updatedUrl, user.getProfilePictureUrl());
        }

        @Test
        @DisplayName("Should handle face descriptor updates")
        void shouldHandleFaceDescriptorUpdates() {
            // Given
            User user = new User();
            String faceData1 = "face_data_vector_1";
            String faceData2 = "face_data_vector_2";

            // When
            user.setFaceDescriptor(faceData1);
            assertEquals(faceData1, user.getFaceDescriptor());

            user.setFaceDescriptor(faceData2);

            // Then
            assertEquals(faceData2, user.getFaceDescriptor());
        }
    }

    @Nested
    @DisplayName("Data Integrity Tests")
    class DataIntegrityTests {

        @Test
        @DisplayName("Should maintain data consistency after multiple updates")
        void shouldMaintainDataConsistencyAfterMultipleUpdates() {
            // Given
            User user = User.builder()
                    .firstName("Original")
                    .lastName("User")
                    .email("original@example.com")
                    .role(Role.FREELANCER)
                    .build();

            // When - Multiple updates
            user.setFirstName("Updated");
            user.setLastName("Person");
            user.setEmail("updated@example.com");
            user.setRole(Role.PROJECT_OWNER);
            user.setEnabled(false);

            // Then
            assertEquals("Updated", user.getFirstName());
            assertEquals("Person", user.getLastName());
            assertEquals("updated@example.com", user.getEmail());
            assertEquals(Role.PROJECT_OWNER, user.getRole());
            assertFalse(user.isEnabled());
        }

        @Test
        @DisplayName("Should handle concurrent field updates")
        void shouldHandleConcurrentFieldUpdates() {
            // Given
            User user = new User();

            // When - Simulate concurrent updates
            user.setFirstName("John");
            user.setEmail("john@example.com");
            user.setRole(Role.ADMIN);
            
            user.setLastName("Doe");
            user.setAddress("123 Main St");
            user.setBirthDate(LocalDate.of(1990, 1, 1));

            // Then - All fields should be properly set
            assertEquals("John", user.getFirstName());
            assertEquals("Doe", user.getLastName());
            assertEquals("john@example.com", user.getEmail());
            assertEquals("123 Main St", user.getAddress());
            assertEquals(Role.ADMIN, user.getRole());
            assertEquals(LocalDate.of(1990, 1, 1), user.getBirthDate());
        }

        @Test
        @DisplayName("Should preserve immutable ID after setting other fields")
        void shouldPreserveImmutableIdAfterSettingOtherFields() {
            // Given
            User user = new User();
            Long originalId = 123L;
            user.setId(originalId);

            // When - Update other fields
            user.setFirstName("Test");
            user.setLastName("User");
            user.setEmail("test@example.com");
            user.setRole(Role.FREELANCER);
            user.setEnabled(false);

            // Then - ID should remain unchanged
            assertEquals(originalId, user.getId());
        }
    }

    @Nested
    @DisplayName("Builder Pattern Advanced Tests")
    class BuilderPatternAdvancedTests {

        @Test
        @DisplayName("Should create multiple different users with same builder class")
        void shouldCreateMultipleDifferentUsersWithSameBuilderClass() {
            // Given & When
            User user1 = User.builder()
                    .firstName("Alice")
                    .email("alice@example.com")
                    .role(Role.ADMIN)
                    .build();

            User user2 = User.builder()
                    .firstName("Bob")
                    .email("bob@example.com")
                    .role(Role.FREELANCER)
                    .build();

            // Then
            assertNotEquals(user1.getFirstName(), user2.getFirstName());
            assertNotEquals(user1.getEmail(), user2.getEmail());
            assertNotEquals(user1.getRole(), user2.getRole());
        }

        @Test
        @DisplayName("Should chain builder methods correctly")
        void shouldChainBuilderMethodsCorrectly() {
            // Given & When
            User user = User.builder()
                    .firstName("Chain")
                    .lastName("Test")
                    .address("Chain Address")
                    .email("chain@test.com")
                    .password("chainpass")
                    .birthDate(LocalDate.of(1995, 6, 15))
                    .role(Role.PROJECT_OWNER)
                    .faceDescriptor("chainface")
                    .profilePictureUrl("/chain.jpg")
                    .enabled(true)
                    .build();

            // Then
            assertEquals("Chain", user.getFirstName());
            assertEquals("Test", user.getLastName());
            assertEquals("Chain Address", user.getAddress());
            assertEquals("chain@test.com", user.getEmail());
            assertEquals("chainpass", user.getPassword());
            assertEquals(LocalDate.of(1995, 6, 15), user.getBirthDate());
            assertEquals(Role.PROJECT_OWNER, user.getRole());
            assertEquals("chainface", user.getFaceDescriptor());
            assertEquals("/chain.jpg", user.getProfilePictureUrl());
            assertTrue(user.isEnabled());
        }

        @Test
        @DisplayName("Should override builder values when called multiple times")
        void shouldOverrideBuilderValuesWhenCalledMultipleTimes() {
            // Given & When
            User user = User.builder()
                    .firstName("First")
                    .firstName("Second")  // Override
                    .email("first@example.com")
                    .email("second@example.com")  // Override
                    .enabled(false)
                    .enabled(true)  // Override
                    .build();

            // Then
            assertEquals("Second", user.getFirstName());
            assertEquals("second@example.com", user.getEmail());
            assertTrue(user.isEnabled());
        }
    }

    @Nested
    @DisplayName("Role Enum Advanced Tests")
    class RoleAdvancedTests {

        @Test
        @DisplayName("Should handle role enum ordinal values")
        void shouldHandleRoleEnumOrdinalValues() {
            // Given & When & Then
            assertEquals(0, Role.ADMIN.ordinal());
            assertEquals(1, Role.PROJECT_OWNER.ordinal());
            assertEquals(2, Role.FREELANCER.ordinal());
        }

        @Test
        @DisplayName("Should handle role enum name method")
        void shouldHandleRoleEnumNameMethod() {
            // Given & When & Then
            assertEquals("ADMIN", Role.ADMIN.name());
            assertEquals("PROJECT_OWNER", Role.PROJECT_OWNER.name());
            assertEquals("FREELANCER", Role.FREELANCER.name());
        }

        @Test
        @DisplayName("Should throw exception for invalid role string")
        void shouldThrowExceptionForInvalidRoleString() {
            // Given & When & Then
            assertThrows(IllegalArgumentException.class, () -> {
                Role.valueOf("INVALID_ROLE");
            });
        }

        @Test
        @DisplayName("Should handle role comparison")
        void shouldHandleRoleComparison() {
            // Given
            Role admin = Role.ADMIN;
            Role projectOwner = Role.PROJECT_OWNER;
            Role freelancer = Role.FREELANCER;

            // When & Then
            assertEquals(admin, Role.ADMIN);
            assertNotEquals(admin, projectOwner);
            assertNotEquals(projectOwner, freelancer);
            
            assertTrue(admin.equals(Role.ADMIN));
            assertFalse(admin.equals(Role.FREELANCER));
        }
    }
}