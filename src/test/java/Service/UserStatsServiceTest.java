package Service;

import DTO.UserStatsResponse;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserStatsService Tests")
class UserStatsServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserStatsService userStatsService;

    private List<User> testUsers;

    @BeforeEach
    void setUp() {
        // Create test users with different roles and statuses
        User admin = User.builder()
                .id(1L)
                .firstName("Admin")
                .lastName("User")
                .email("admin@example.com")
                .role(Role.ADMIN)
                .enabled(true)
                .build();

        User activeFreelancer1 = User.builder()
                .id(2L)
                .firstName("John")
                .lastName("Freelancer")
                .email("john.freelancer@example.com")
                .role(Role.FREELANCER)
                .enabled(true)
                .build();

        User activeFreelancer2 = User.builder()
                .id(3L)
                .firstName("Jane")
                .lastName("Freelancer")
                .email("jane.freelancer@example.com")
                .role(Role.FREELANCER)
                .enabled(true)
                .build();

        User inactiveFreelancer = User.builder()
                .id(4L)
                .firstName("Bob")
                .lastName("Freelancer")
                .email("bob.freelancer@example.com")
                .role(Role.FREELANCER)
                .enabled(false)
                .build();

        User activeProjectOwner1 = User.builder()
                .id(5L)
                .firstName("Alice")
                .lastName("Owner")
                .email("alice.owner@example.com")
                .role(Role.PROJECT_OWNER)
                .enabled(true)
                .build();

        User activeProjectOwner2 = User.builder()
                .id(6L)
                .firstName("Charlie")
                .lastName("Owner")
                .email("charlie.owner@example.com")
                .role(Role.PROJECT_OWNER)
                .enabled(true)
                .build();

        User inactiveProjectOwner = User.builder()
                .id(7L)
                .firstName("David")
                .lastName("Owner")
                .email("david.owner@example.com")
                .role(Role.PROJECT_OWNER)
                .enabled(false)
                .build();

        testUsers = Arrays.asList(
                admin,
                activeFreelancer1,
                activeFreelancer2,
                inactiveFreelancer,
                activeProjectOwner1,
                activeProjectOwner2,
                inactiveProjectOwner
        );
    }

    @Nested
    @DisplayName("Get User Statistics Tests")
    class GetUserStatisticsTests {

        @Test
        @DisplayName("Should calculate user statistics correctly")
        void shouldCalculateUserStatisticsCorrectly() {
            // Given
            when(userRepository.findAll()).thenReturn(testUsers);

            // When
            UserStatsResponse result = userStatsService.getUserStatistics();

            // Then
            assertNotNull(result);
            assertEquals(7L, result.getTotalUsers()); // All users
            assertEquals(2L, result.getActiveFreelancers()); // 2 active freelancers
            assertEquals(2L, result.getActiveProjectOwners()); // 2 active project owners
            assertEquals(5L, result.getActiveAccounts()); // 1 admin + 2 freelancers + 2 project owners
            assertEquals(2L, result.getInactiveAccounts()); // 1 inactive freelancer + 1 inactive project owner
            assertEquals(1L, result.getTotalAdmins()); // 1 admin

            verify(userRepository).findAll();
        }

        @Test
        @DisplayName("Should handle empty user list")
        void shouldHandleEmptyUserList() {
            // Given
            when(userRepository.findAll()).thenReturn(Collections.emptyList());

            // When
            UserStatsResponse result = userStatsService.getUserStatistics();

            // Then
            assertNotNull(result);
            assertEquals(0L, result.getTotalUsers());
            assertEquals(0L, result.getActiveFreelancers());
            assertEquals(0L, result.getActiveProjectOwners());
            assertEquals(0L, result.getActiveAccounts());
            assertEquals(0L, result.getInactiveAccounts());
            assertEquals(0L, result.getTotalAdmins());

            verify(userRepository).findAll();
        }

        @Test
        @DisplayName("Should handle only admin users")
        void shouldHandleOnlyAdminUsers() {
            // Given
            User admin1 = User.builder()
                    .id(1L)
                    .firstName("Admin1")
                    .lastName("User")
                    .email("admin1@example.com")
                    .role(Role.ADMIN)
                    .enabled(true)
                    .build();

            User admin2 = User.builder()
                    .id(2L)
                    .firstName("Admin2")
                    .lastName("User")
                    .email("admin2@example.com")
                    .role(Role.ADMIN)
                    .enabled(true)
                    .build();

            List<User> adminUsers = Arrays.asList(admin1, admin2);
            when(userRepository.findAll()).thenReturn(adminUsers);

            // When
            UserStatsResponse result = userStatsService.getUserStatistics();

            // Then
            assertNotNull(result);
            assertEquals(2L, result.getTotalUsers());
            assertEquals(0L, result.getActiveFreelancers());
            assertEquals(0L, result.getActiveProjectOwners());
            assertEquals(2L, result.getActiveAccounts());
            assertEquals(0L, result.getInactiveAccounts());
            assertEquals(2L, result.getTotalAdmins());

            verify(userRepository).findAll();
        }

        @Test
        @DisplayName("Should handle only freelancers")
        void shouldHandleOnlyFreelancers() {
            // Given
            User freelancer1 = User.builder()
                    .id(1L)
                    .firstName("John")
                    .lastName("Freelancer")
                    .email("john@example.com")
                    .role(Role.FREELANCER)
                    .enabled(true)
                    .build();

            User freelancer2 = User.builder()
                    .id(2L)
                    .firstName("Jane")
                    .lastName("Freelancer")
                    .email("jane@example.com")
                    .role(Role.FREELANCER)
                    .enabled(false)
                    .build();

            List<User> freelancers = Arrays.asList(freelancer1, freelancer2);
            when(userRepository.findAll()).thenReturn(freelancers);

            // When
            UserStatsResponse result = userStatsService.getUserStatistics();

            // Then
            assertNotNull(result);
            assertEquals(2L, result.getTotalUsers());
            assertEquals(1L, result.getActiveFreelancers());
            assertEquals(0L, result.getActiveProjectOwners());
            assertEquals(1L, result.getActiveAccounts());
            assertEquals(1L, result.getInactiveAccounts());
            assertEquals(0L, result.getTotalAdmins());

            verify(userRepository).findAll();
        }

        @Test
        @DisplayName("Should handle only project owners")
        void shouldHandleOnlyProjectOwners() {
            // Given
            User owner1 = User.builder()
                    .id(1L)
                    .firstName("Alice")
                    .lastName("Owner")
                    .email("alice@example.com")
                    .role(Role.PROJECT_OWNER)
                    .enabled(true)
                    .build();

            User owner2 = User.builder()
                    .id(2L)
                    .firstName("Bob")
                    .lastName("Owner")
                    .email("bob@example.com")
                    .role(Role.PROJECT_OWNER)
                    .enabled(false)
                    .build();

            List<User> owners = Arrays.asList(owner1, owner2);
            when(userRepository.findAll()).thenReturn(owners);

            // When
            UserStatsResponse result = userStatsService.getUserStatistics();

            // Then
            assertNotNull(result);
            assertEquals(2L, result.getTotalUsers());
            assertEquals(0L, result.getActiveFreelancers());
            assertEquals(1L, result.getActiveProjectOwners());
            assertEquals(1L, result.getActiveAccounts());
            assertEquals(1L, result.getInactiveAccounts());
            assertEquals(0L, result.getTotalAdmins());

            verify(userRepository).findAll();
        }

        @Test
        @DisplayName("Should handle all inactive users")
        void shouldHandleAllInactiveUsers() {
            // Given
            User inactiveAdmin = User.builder()
                    .id(1L)
                    .firstName("Admin")
                    .lastName("User")
                    .email("admin@example.com")
                    .role(Role.ADMIN)
                    .enabled(false)
                    .build();

            User inactiveFreelancer = User.builder()
                    .id(2L)
                    .firstName("John")
                    .lastName("Freelancer")
                    .email("john@example.com")
                    .role(Role.FREELANCER)
                    .enabled(false)
                    .build();

            User inactiveOwner = User.builder()
                    .id(3L)
                    .firstName("Alice")
                    .lastName("Owner")
                    .email("alice@example.com")
                    .role(Role.PROJECT_OWNER)
                    .enabled(false)
                    .build();

            List<User> inactiveUsers = Arrays.asList(inactiveAdmin, inactiveFreelancer, inactiveOwner);
            when(userRepository.findAll()).thenReturn(inactiveUsers);

            // When
            UserStatsResponse result = userStatsService.getUserStatistics();

            // Then
            assertNotNull(result);
            assertEquals(3L, result.getTotalUsers());
            assertEquals(0L, result.getActiveFreelancers());
            assertEquals(0L, result.getActiveProjectOwners());
            assertEquals(0L, result.getActiveAccounts());
            assertEquals(3L, result.getInactiveAccounts());
            assertEquals(1L, result.getTotalAdmins());

            verify(userRepository).findAll();
        }
    }

    @Nested
    @DisplayName("Get Statistics By Date Range Tests")
    class GetStatisticsByDateRangeTests {

        @Test
        @DisplayName("Should calculate statistics by date range")
        void shouldCalculateStatisticsByDateRange() {
            // Given
            LocalDateTime startDate = LocalDateTime.now().minusDays(30);
            LocalDateTime endDate = LocalDateTime.now();
            when(userRepository.findAll()).thenReturn(testUsers);

            // When
            UserStatsResponse result = userStatsService.getStatisticsByDateRange(startDate, endDate);

            // Then
            assertNotNull(result);
            assertEquals(7L, result.getTotalUsers());
            assertEquals(2L, result.getActiveFreelancers());
            assertEquals(2L, result.getActiveProjectOwners());
            assertEquals(5L, result.getActiveAccounts());
            assertEquals(2L, result.getInactiveAccounts());
            assertEquals(1L, result.getTotalAdmins());

            verify(userRepository).findAll();
        }

        @Test
        @DisplayName("Should handle empty date range results")
        void shouldHandleEmptyDateRangeResults() {
            // Given
            LocalDateTime startDate = LocalDateTime.now().minusDays(30);
            LocalDateTime endDate = LocalDateTime.now();
            when(userRepository.findAll()).thenReturn(Collections.emptyList());

            // When
            UserStatsResponse result = userStatsService.getStatisticsByDateRange(startDate, endDate);

            // Then
            assertNotNull(result);
            assertEquals(0L, result.getTotalUsers());
            assertEquals(0L, result.getActiveFreelancers());
            assertEquals(0L, result.getActiveProjectOwners());
            assertEquals(0L, result.getActiveAccounts());
            assertEquals(0L, result.getInactiveAccounts());
            assertEquals(0L, result.getTotalAdmins());

            verify(userRepository).findAll();
        }

        @Test
        @DisplayName("Should handle null date parameters")
        void shouldHandleNullDateParameters() {
            // Given
            when(userRepository.findAll()).thenReturn(testUsers);

            // When
            UserStatsResponse result = userStatsService.getStatisticsByDateRange(null, null);

            // Then
            assertNotNull(result);
            assertEquals(7L, result.getTotalUsers());
            assertEquals(2L, result.getActiveFreelancers());
            assertEquals(2L, result.getActiveProjectOwners());
            assertEquals(5L, result.getActiveAccounts());
            assertEquals(2L, result.getInactiveAccounts());
            assertEquals(1L, result.getTotalAdmins());

            verify(userRepository).findAll();
        }

        @Test
        @DisplayName("Should handle future date range")
        void shouldHandleFutureDateRange() {
            // Given
            LocalDateTime startDate = LocalDateTime.now().plusDays(1);
            LocalDateTime endDate = LocalDateTime.now().plusDays(30);
            when(userRepository.findAll()).thenReturn(testUsers);

            // When
            UserStatsResponse result = userStatsService.getStatisticsByDateRange(startDate, endDate);

            // Then
            assertNotNull(result);
            // Since we don't have creation date filtering implemented, 
            // it should return all users
            assertEquals(7L, result.getTotalUsers());

            verify(userRepository).findAll();
        }

        @Test
        @DisplayName("Should handle same start and end date")
        void shouldHandleSameStartAndEndDate() {
            // Given
            LocalDateTime sameDate = LocalDateTime.now();
            when(userRepository.findAll()).thenReturn(testUsers);

            // When
            UserStatsResponse result = userStatsService.getStatisticsByDateRange(sameDate, sameDate);

            // Then
            assertNotNull(result);
            assertEquals(7L, result.getTotalUsers());
            assertEquals(2L, result.getActiveFreelancers());
            assertEquals(2L, result.getActiveProjectOwners());
            assertEquals(5L, result.getActiveAccounts());
            assertEquals(2L, result.getInactiveAccounts());
            assertEquals(1L, result.getTotalAdmins());

            verify(userRepository).findAll();
        }
    }

    @Nested
    @DisplayName("Edge Cases Tests")
    class EdgeCasesTests {

        @Test
        @DisplayName("Should handle repository exception")
        void shouldHandleRepositoryException() {
            // Given
            when(userRepository.findAll()).thenThrow(new RuntimeException("Database error"));

            // When & Then
            assertThrows(RuntimeException.class, () -> userStatsService.getUserStatistics());
            verify(userRepository).findAll();
        }

        @Test
        @DisplayName("Should handle users with null roles")
        void shouldHandleUsersWithNullRoles() {
            // Given
            User userWithNullRole = User.builder()
                    .id(1L)
                    .firstName("Test")
                    .lastName("User")
                    .email("test@example.com")
                    .role(null)
                    .enabled(true)
                    .build();

            when(userRepository.findAll()).thenReturn(Arrays.asList(userWithNullRole));

            // When
            UserStatsResponse result = userStatsService.getUserStatistics();

            // Then
            assertNotNull(result);
            assertEquals(1L, result.getTotalUsers());
            assertEquals(0L, result.getActiveFreelancers());
            assertEquals(0L, result.getActiveProjectOwners());
            assertEquals(1L, result.getActiveAccounts()); // Still counts as active
            assertEquals(0L, result.getInactiveAccounts());
            assertEquals(0L, result.getTotalAdmins());

            verify(userRepository).findAll();
        }

        @Test
        @DisplayName("Should handle large number of users")
        void shouldHandleLargeNumberOfUsers() {
            // Given
            List<User> largeUserList = new java.util.ArrayList<>();
            for (int i = 0; i < 10000; i++) {
                User user = User.builder()
                        .id((long) i)
                        .firstName("User" + i)
                        .lastName("Test")
                        .email("user" + i + "@example.com")
                        .role(i % 3 == 0 ? Role.ADMIN : (i % 2 == 0 ? Role.FREELANCER : Role.PROJECT_OWNER))
                        .enabled(i % 4 != 0) // 75% enabled
                        .build();
                largeUserList.add(user);
            }

            when(userRepository.findAll()).thenReturn(largeUserList);

            // When
            UserStatsResponse result = userStatsService.getUserStatistics();

            // Then
            assertNotNull(result);
            assertEquals(10000L, result.getTotalUsers());
            assertTrue(result.getActiveAccounts() > 0);
            assertTrue(result.getInactiveAccounts() > 0);
            assertTrue(result.getTotalAdmins() > 0);

            verify(userRepository).findAll();
        }
    }
}