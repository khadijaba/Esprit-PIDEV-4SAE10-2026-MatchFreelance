package Repository;

import Entity.Role;
import Entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("UserRepository Tests")
class UserRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private UserRepository userRepository;

    private User testUser1;
    private User testUser2;
    private User testUser3;

    @BeforeEach
    void setUp() {
        // Create test users
        testUser1 = User.builder()
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@example.com")
                .password("password123")
                .address("123 Main St")
                .birthDate(LocalDate.of(1990, 1, 1))
                .role(Role.FREELANCER)
                .enabled(true)
                .faceDescriptor("face123")
                .profilePictureUrl("/images/john.jpg")
                .build();

        testUser2 = User.builder()
                .firstName("Jane")
                .lastName("Smith")
                .email("jane.smith@example.com")
                .password("password456")
                .address("456 Oak Ave")
                .birthDate(LocalDate.of(1985, 5, 15))
                .role(Role.PROJECT_OWNER)
                .enabled(false)
                .faceDescriptor("face456")
                .profilePictureUrl("/images/jane.jpg")
                .build();

        testUser3 = User.builder()
                .firstName("Bob")
                .lastName("Johnson")
                .email("bob.johnson@example.com")
                .password("password789")
                .address("789 Pine St")
                .birthDate(LocalDate.of(1980, 12, 25))
                .role(Role.ADMIN)
                .enabled(true)
                .faceDescriptor("face789")
                .profilePictureUrl("/images/bob.jpg")
                .build();

        // Persist test users
        entityManager.persistAndFlush(testUser1);
        entityManager.persistAndFlush(testUser2);
        entityManager.persistAndFlush(testUser3);
    }

    @Nested
    @DisplayName("Find By Email Tests")
    class FindByEmailTests {

        @Test
        @DisplayName("Should find user by email successfully")
        void shouldFindUserByEmailSuccessfully() {
            // When
            Optional<User> result = userRepository.findByEmail("john.doe@example.com");

            // Then
            assertTrue(result.isPresent());
            assertEquals("John", result.get().getFirstName());
            assertEquals("Doe", result.get().getLastName());
            assertEquals("john.doe@example.com", result.get().getEmail());
            assertEquals(Role.FREELANCER, result.get().getRole());
        }

        @Test
        @DisplayName("Should return empty optional when email not found")
        void shouldReturnEmptyOptionalWhenEmailNotFound() {
            // When
            Optional<User> result = userRepository.findByEmail("nonexistent@example.com");

            // Then
            assertFalse(result.isPresent());
        }

        @Test
        @DisplayName("Should handle null email")
        void shouldHandleNullEmail() {
            // When
            Optional<User> result = userRepository.findByEmail(null);

            // Then
            assertFalse(result.isPresent());
        }

        @Test
        @DisplayName("Should handle empty email")
        void shouldHandleEmptyEmail() {
            // When
            Optional<User> result = userRepository.findByEmail("");

            // Then
            assertFalse(result.isPresent());
        }

        @Test
        @DisplayName("Should be case sensitive for email search")
        void shouldBeCaseSensitiveForEmailSearch() {
            // When
            Optional<User> result = userRepository.findByEmail("JOHN.DOE@EXAMPLE.COM");

            // Then
            assertFalse(result.isPresent());
        }
    }

    @Nested
    @DisplayName("Exists By Email Tests")
    class ExistsByEmailTests {

        @Test
        @DisplayName("Should return true when email exists")
        void shouldReturnTrueWhenEmailExists() {
            // When
            boolean exists = userRepository.existsByEmail("jane.smith@example.com");

            // Then
            assertTrue(exists);
        }

        @Test
        @DisplayName("Should return false when email does not exist")
        void shouldReturnFalseWhenEmailDoesNotExist() {
            // When
            boolean exists = userRepository.existsByEmail("nonexistent@example.com");

            // Then
            assertFalse(exists);
        }

        @Test
        @DisplayName("Should return false for null email")
        void shouldReturnFalseForNullEmail() {
            // When
            boolean exists = userRepository.existsByEmail(null);

            // Then
            assertFalse(exists);
        }

        @Test
        @DisplayName("Should return false for empty email")
        void shouldReturnFalseForEmptyEmail() {
            // When
            boolean exists = userRepository.existsByEmail("");

            // Then
            assertFalse(exists);
        }
    }

    @Nested
    @DisplayName("Exists By Role Tests")
    class ExistsByRoleTests {

        @Test
        @DisplayName("Should return true when role exists")
        void shouldReturnTrueWhenRoleExists() {
            // When
            boolean exists = userRepository.existsByRole(Role.ADMIN);

            // Then
            assertTrue(exists);
        }

        @Test
        @DisplayName("Should return false when role does not exist")
        void shouldReturnFalseWhenRoleDoesNotExist() {
            // Given - Remove all users first
            userRepository.deleteAll();
            entityManager.flush();

            // When
            boolean exists = userRepository.existsByRole(Role.FREELANCER);

            // Then
            assertFalse(exists);
        }

        @Test
        @DisplayName("Should return false for null role")
        void shouldReturnFalseForNullRole() {
            // When
            boolean exists = userRepository.existsByRole(null);

            // Then
            assertFalse(exists);
        }
    }

    @Nested
    @DisplayName("Find By Role Tests")
    class FindByRoleTests {

        @Test
        @DisplayName("Should find users by role successfully")
        void shouldFindUsersByRoleSuccessfully() {
            // When
            List<User> freelancers = userRepository.findByRole(Role.FREELANCER);

            // Then
            assertEquals(1, freelancers.size());
            assertEquals("John", freelancers.get(0).getFirstName());
            assertEquals(Role.FREELANCER, freelancers.get(0).getRole());
        }

        @Test
        @DisplayName("Should return empty list when no users found for role")
        void shouldReturnEmptyListWhenNoUsersFoundForRole() {
            // Given - Remove all users first
            userRepository.deleteAll();
            entityManager.flush();

            // When
            List<User> users = userRepository.findByRole(Role.FREELANCER);

            // Then
            assertTrue(users.isEmpty());
        }

        @Test
        @DisplayName("Should find multiple users with same role")
        void shouldFindMultipleUsersWithSameRole() {
            // Given - Add another freelancer
            User anotherFreelancer = User.builder()
                    .firstName("Alice")
                    .lastName("Brown")
                    .email("alice.brown@example.com")
                    .password("password000")
                    .address("000 Elm St")
                    .birthDate(LocalDate.of(1995, 3, 10))
                    .role(Role.FREELANCER)
                    .enabled(true)
                    .build();
            entityManager.persistAndFlush(anotherFreelancer);

            // When
            List<User> freelancers = userRepository.findByRole(Role.FREELANCER);

            // Then
            assertEquals(2, freelancers.size());
            assertTrue(freelancers.stream().allMatch(user -> user.getRole() == Role.FREELANCER));
        }

        @Test
        @DisplayName("Should handle null role")
        void shouldHandleNullRole() {
            // When
            List<User> users = userRepository.findByRole(null);

            // Then
            assertTrue(users.isEmpty());
        }
    }

    @Nested
    @DisplayName("Find By Enabled Tests")
    class FindByEnabledTests {

        @Test
        @DisplayName("Should find enabled users successfully")
        void shouldFindEnabledUsersSuccessfully() {
            // When
            List<User> enabledUsers = userRepository.findByEnabled(true);

            // Then
            assertEquals(2, enabledUsers.size());
            assertTrue(enabledUsers.stream().allMatch(User::isEnabled));
        }

        @Test
        @DisplayName("Should find disabled users successfully")
        void shouldFindDisabledUsersSuccessfully() {
            // When
            List<User> disabledUsers = userRepository.findByEnabled(false);

            // Then
            assertEquals(1, disabledUsers.size());
            assertFalse(disabledUsers.get(0).isEnabled());
            assertEquals("Jane", disabledUsers.get(0).getFirstName());
        }

        @Test
        @DisplayName("Should return empty list when no users match enabled status")
        void shouldReturnEmptyListWhenNoUsersMatchEnabledStatus() {
            // Given - Enable all users
            testUser2.setEnabled(true);
            entityManager.persistAndFlush(testUser2);

            // When
            List<User> disabledUsers = userRepository.findByEnabled(false);

            // Then
            assertTrue(disabledUsers.isEmpty());
        }
    }

    @Nested
    @DisplayName("JPA Specification Tests")
    class JpaSpecificationTests {

        @Test
        @DisplayName("Should find users with specification")
        void shouldFindUsersWithSpecification() {
            // Given
            Specification<User> spec = (root, query, criteriaBuilder) ->
                    criteriaBuilder.equal(root.get("role"), Role.FREELANCER);

            // When
            List<User> result = userRepository.findAll(spec);

            // Then
            assertEquals(1, result.size());
            assertEquals(Role.FREELANCER, result.get(0).getRole());
        }

        @Test
        @DisplayName("Should find users with complex specification")
        void shouldFindUsersWithComplexSpecification() {
            // Given
            Specification<User> spec = (root, query, criteriaBuilder) ->
                    criteriaBuilder.and(
                            criteriaBuilder.equal(root.get("enabled"), true),
                            criteriaBuilder.notEqual(root.get("role"), Role.ADMIN)
                    );

            // When
            List<User> result = userRepository.findAll(spec);

            // Then
            assertEquals(1, result.size());
            assertEquals("John", result.get(0).getFirstName());
            assertTrue(result.get(0).isEnabled());
            assertNotEquals(Role.ADMIN, result.get(0).getRole());
        }

        @Test
        @DisplayName("Should find users with specification and pagination")
        void shouldFindUsersWithSpecificationAndPagination() {
            // Given
            Specification<User> spec = (root, query, criteriaBuilder) ->
                    criteriaBuilder.equal(root.get("enabled"), true);
            Pageable pageable = PageRequest.of(0, 1);

            // When
            Page<User> result = userRepository.findAll(spec, pageable);

            // Then
            assertEquals(1, result.getContent().size());
            assertEquals(2, result.getTotalElements());
            assertEquals(2, result.getTotalPages());
            assertTrue(result.getContent().get(0).isEnabled());
        }

        @Test
        @DisplayName("Should find users with like specification")
        void shouldFindUsersWithLikeSpecification() {
            // Given
            Specification<User> spec = (root, query, criteriaBuilder) ->
                    criteriaBuilder.like(root.get("firstName"), "%oh%");

            // When
            List<User> result = userRepository.findAll(spec);

            // Then
            assertEquals(2, result.size()); // John and Bob contain "oh"
        }

        @Test
        @DisplayName("Should return empty result with non-matching specification")
        void shouldReturnEmptyResultWithNonMatchingSpecification() {
            // Given
            Specification<User> spec = (root, query, criteriaBuilder) ->
                    criteriaBuilder.equal(root.get("firstName"), "NonExistent");

            // When
            List<User> result = userRepository.findAll(spec);

            // Then
            assertTrue(result.isEmpty());
        }
    }

    @Nested
    @DisplayName("CRUD Operations Tests")
    class CrudOperationsTests {

        @Test
        @DisplayName("Should save user successfully")
        void shouldSaveUserSuccessfully() {
            // Given
            User newUser = User.builder()
                    .firstName("Charlie")
                    .lastName("Wilson")
                    .email("charlie.wilson@example.com")
                    .password("passwordCharlie")
                    .address("999 Cedar St")
                    .birthDate(LocalDate.of(1992, 8, 20))
                    .role(Role.PROJECT_OWNER)
                    .enabled(true)
                    .build();

            // When
            User savedUser = userRepository.save(newUser);

            // Then
            assertNotNull(savedUser.getId());
            assertEquals("Charlie", savedUser.getFirstName());
            assertEquals("Wilson", savedUser.getLastName());
            assertEquals("charlie.wilson@example.com", savedUser.getEmail());
            assertEquals(Role.PROJECT_OWNER, savedUser.getRole());
            assertTrue(savedUser.isEnabled());
        }

        @Test
        @DisplayName("Should update user successfully")
        void shouldUpdateUserSuccessfully() {
            // Given
            testUser1.setFirstName("UpdatedJohn");
            testUser1.setEnabled(false);

            // When
            User updatedUser = userRepository.save(testUser1);

            // Then
            assertEquals("UpdatedJohn", updatedUser.getFirstName());
            assertFalse(updatedUser.isEnabled());
            assertEquals(testUser1.getId(), updatedUser.getId());
        }

        @Test
        @DisplayName("Should delete user successfully")
        void shouldDeleteUserSuccessfully() {
            // Given
            Long userId = testUser1.getId();

            // When
            userRepository.deleteById(userId);
            entityManager.flush();

            // Then
            Optional<User> deletedUser = userRepository.findById(userId);
            assertFalse(deletedUser.isPresent());
        }

        @Test
        @DisplayName("Should find user by id successfully")
        void shouldFindUserByIdSuccessfully() {
            // When
            Optional<User> result = userRepository.findById(testUser2.getId());

            // Then
            assertTrue(result.isPresent());
            assertEquals("Jane", result.get().getFirstName());
            assertEquals("Smith", result.get().getLastName());
        }

        @Test
        @DisplayName("Should return empty optional for non-existent id")
        void shouldReturnEmptyOptionalForNonExistentId() {
            // When
            Optional<User> result = userRepository.findById(999L);

            // Then
            assertFalse(result.isPresent());
        }

        @Test
        @DisplayName("Should count users correctly")
        void shouldCountUsersCorrectly() {
            // When
            long count = userRepository.count();

            // Then
            assertEquals(3, count);
        }

        @Test
        @DisplayName("Should find all users successfully")
        void shouldFindAllUsersSuccessfully() {
            // When
            List<User> allUsers = userRepository.findAll();

            // Then
            assertEquals(3, allUsers.size());
        }
    }

    @Nested
    @DisplayName("Edge Cases Tests")
    class EdgeCasesTests {

        @Test
        @DisplayName("Should handle user with minimal data")
        void shouldHandleUserWithMinimalData() {
            // Given
            User minimalUser = User.builder()
                    .firstName("Min")
                    .lastName("User")
                    .email("min@example.com")
                    .password("minpass")
                    .address("Min Address")
                    .birthDate(LocalDate.now())
                    .role(Role.FREELANCER)
                    .enabled(true)
                    .build();

            // When
            User savedUser = userRepository.save(minimalUser);

            // Then
            assertNotNull(savedUser.getId());
            assertEquals("Min", savedUser.getFirstName());
            assertNull(savedUser.getFaceDescriptor());
            assertNull(savedUser.getProfilePictureUrl());
        }

        @Test
        @DisplayName("Should handle user with maximum length strings")
        void shouldHandleUserWithMaximumLengthStrings() {
            // Given
            String longString = "a".repeat(255);
            User longStringUser = User.builder()
                    .firstName(longString)
                    .lastName(longString)
                    .email("long@example.com")
                    .password(longString)
                    .address(longString)
                    .birthDate(LocalDate.now())
                    .role(Role.FREELANCER)
                    .enabled(true)
                    .profilePictureUrl(longString)
                    .build();

            // When
            User savedUser = userRepository.save(longStringUser);

            // Then
            assertNotNull(savedUser.getId());
            assertEquals(longString, savedUser.getFirstName());
            assertEquals(longString, savedUser.getLastName());
            assertEquals(longString, savedUser.getProfilePictureUrl());
        }

        @Test
        @DisplayName("Should handle special characters in user data")
        void shouldHandleSpecialCharactersInUserData() {
            // Given
            String specialChars = "Jöhn Döé 测试 🚀";
            User specialUser = User.builder()
                    .firstName(specialChars)
                    .lastName(specialChars)
                    .email("special@example.com")
                    .password("password")
                    .address(specialChars)
                    .birthDate(LocalDate.now())
                    .role(Role.FREELANCER)
                    .enabled(true)
                    .build();

            // When
            User savedUser = userRepository.save(specialUser);

            // Then
            assertNotNull(savedUser.getId());
            assertEquals(specialChars, savedUser.getFirstName());
            assertEquals(specialChars, savedUser.getLastName());
            assertEquals(specialChars, savedUser.getAddress());
        }

        @Test
        @DisplayName("Should handle very old and future birth dates")
        void shouldHandleVeryOldAndFutureBirthDates() {
            // Given
            User oldUser = User.builder()
                    .firstName("Old")
                    .lastName("User")
                    .email("old@example.com")
                    .password("password")
                    .address("Old Address")
                    .birthDate(LocalDate.of(1800, 1, 1))
                    .role(Role.FREELANCER)
                    .enabled(true)
                    .build();

            User futureUser = User.builder()
                    .firstName("Future")
                    .lastName("User")
                    .email("future@example.com")
                    .password("password")
                    .address("Future Address")
                    .birthDate(LocalDate.of(2100, 12, 31))
                    .role(Role.FREELANCER)
                    .enabled(true)
                    .build();

            // When
            User savedOldUser = userRepository.save(oldUser);
            User savedFutureUser = userRepository.save(futureUser);

            // Then
            assertEquals(LocalDate.of(1800, 1, 1), savedOldUser.getBirthDate());
            assertEquals(LocalDate.of(2100, 12, 31), savedFutureUser.getBirthDate());
        }
    }
}