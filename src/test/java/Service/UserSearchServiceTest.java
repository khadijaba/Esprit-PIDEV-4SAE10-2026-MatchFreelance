package Service;

import DTO.UserFilterRequest;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserSearchService Tests")
class UserSearchServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserSearchService userSearchService;

    private List<User> testUsers;
    private UserFilterRequest filterRequest;

    @BeforeEach
    void setUp() {
        // Create test users
        User user1 = User.builder()
                .id(1L)
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@example.com")
                .role(Role.FREELANCER)
                .enabled(true)
                .birthDate(LocalDate.of(1990, 1, 1))
                .build();

        User user2 = User.builder()
                .id(2L)
                .firstName("Jane")
                .lastName("Smith")
                .email("jane.smith@example.com")
                .role(Role.PROJECT_OWNER)
                .enabled(false)
                .birthDate(LocalDate.of(1985, 5, 15))
                .build();

        User user3 = User.builder()
                .id(3L)
                .firstName("Bob")
                .lastName("Johnson")
                .email("bob.johnson@example.com")
                .role(Role.ADMIN)
                .enabled(true)
                .birthDate(LocalDate.of(1980, 12, 25))
                .build();

        testUsers = Arrays.asList(user1, user2, user3);

        // Create default filter request
        filterRequest = new UserFilterRequest();
        filterRequest.setPage(0);
        filterRequest.setSize(10);
        filterRequest.setSortBy("firstName");
        filterRequest.setSortDir("ASC");
    }

    @Nested
    @DisplayName("Search Users Tests")
    class SearchUsersTests {

        @Test
        @DisplayName("Should search users with all filters")
        void shouldSearchUsersWithAllFilters() {
            // Given
            filterRequest.setName("John");
            filterRequest.setEmail("john.doe");
            filterRequest.setRole(Role.FREELANCER);
            filterRequest.setEnabled(true);

            Page<User> expectedPage = new PageImpl<>(Arrays.asList(testUsers.get(0)));
            when(userRepository.findAll(any(Specification.class), any(Pageable.class)))
                    .thenReturn(expectedPage);

            // When
            Page<User> result = userSearchService.searchUsers(filterRequest);

            // Then
            assertNotNull(result);
            assertEquals(1, result.getContent().size());
            assertEquals("John", result.getContent().get(0).getFirstName());
            verify(userRepository).findAll(any(Specification.class), any(Pageable.class));
        }

        @Test
        @DisplayName("Should search users by name only")
        void shouldSearchUsersByNameOnly() {
            // Given
            filterRequest.setName("Jane");
            
            Page<User> expectedPage = new PageImpl<>(Arrays.asList(testUsers.get(1)));
            when(userRepository.findAll(any(Specification.class), any(Pageable.class)))
                    .thenReturn(expectedPage);

            // When
            Page<User> result = userSearchService.searchUsers(filterRequest);

            // Then
            assertNotNull(result);
            assertEquals(1, result.getContent().size());
            assertEquals("Jane", result.getContent().get(0).getFirstName());
            verify(userRepository).findAll(any(Specification.class), any(Pageable.class));
        }

        @Test
        @DisplayName("Should search users by email only")
        void shouldSearchUsersByEmailOnly() {
            // Given
            filterRequest.setEmail("smith");
            
            Page<User> expectedPage = new PageImpl<>(Arrays.asList(testUsers.get(1)));
            when(userRepository.findAll(any(Specification.class), any(Pageable.class)))
                    .thenReturn(expectedPage);

            // When
            Page<User> result = userSearchService.searchUsers(filterRequest);

            // Then
            assertNotNull(result);
            assertEquals(1, result.getContent().size());
            assertEquals("jane.smith@example.com", result.getContent().get(0).getEmail());
            verify(userRepository).findAll(any(Specification.class), any(Pageable.class));
        }

        @Test
        @DisplayName("Should search users by role only")
        void shouldSearchUsersByRoleOnly() {
            // Given
            filterRequest.setRole(Role.ADMIN);
            
            Page<User> expectedPage = new PageImpl<>(Arrays.asList(testUsers.get(2)));
            when(userRepository.findAll(any(Specification.class), any(Pageable.class)))
                    .thenReturn(expectedPage);

            // When
            Page<User> result = userSearchService.searchUsers(filterRequest);

            // Then
            assertNotNull(result);
            assertEquals(1, result.getContent().size());
            assertEquals(Role.ADMIN, result.getContent().get(0).getRole());
            verify(userRepository).findAll(any(Specification.class), any(Pageable.class));
        }

        @Test
        @DisplayName("Should search users by enabled status only")
        void shouldSearchUsersByEnabledStatusOnly() {
            // Given
            filterRequest.setEnabled(false);
            
            Page<User> expectedPage = new PageImpl<>(Arrays.asList(testUsers.get(1)));
            when(userRepository.findAll(any(Specification.class), any(Pageable.class)))
                    .thenReturn(expectedPage);

            // When
            Page<User> result = userSearchService.searchUsers(filterRequest);

            // Then
            assertNotNull(result);
            assertEquals(1, result.getContent().size());
            assertFalse(result.getContent().get(0).isEnabled());
            verify(userRepository).findAll(any(Specification.class), any(Pageable.class));
        }

        @Test
        @DisplayName("Should search users with no filters")
        void shouldSearchUsersWithNoFilters() {
            // Given
            Page<User> expectedPage = new PageImpl<>(testUsers);
            when(userRepository.findAll(any(Specification.class), any(Pageable.class)))
                    .thenReturn(expectedPage);

            // When
            Page<User> result = userSearchService.searchUsers(filterRequest);

            // Then
            assertNotNull(result);
            assertEquals(3, result.getContent().size());
            verify(userRepository).findAll(any(Specification.class), any(Pageable.class));
        }

        @Test
        @DisplayName("Should handle empty search results")
        void shouldHandleEmptySearchResults() {
            // Given
            filterRequest.setName("NonExistentUser");
            
            Page<User> expectedPage = new PageImpl<>(Collections.emptyList());
            when(userRepository.findAll(any(Specification.class), any(Pageable.class)))
                    .thenReturn(expectedPage);

            // When
            Page<User> result = userSearchService.searchUsers(filterRequest);

            // Then
            assertNotNull(result);
            assertEquals(0, result.getContent().size());
            verify(userRepository).findAll(any(Specification.class), any(Pageable.class));
        }

        @Test
        @DisplayName("Should handle pagination correctly")
        void shouldHandlePaginationCorrectly() {
            // Given
            filterRequest.setPage(1);
            filterRequest.setSize(2);
            
            Page<User> expectedPage = new PageImpl<>(Arrays.asList(testUsers.get(2)), 
                    PageRequest.of(1, 2), 3);
            when(userRepository.findAll(any(Specification.class), any(Pageable.class)))
                    .thenReturn(expectedPage);

            // When
            Page<User> result = userSearchService.searchUsers(filterRequest);

            // Then
            assertNotNull(result);
            assertEquals(1, result.getContent().size());
            assertEquals(1, result.getNumber());
            assertEquals(2, result.getSize());
            assertEquals(3, result.getTotalElements());
            verify(userRepository).findAll(any(Specification.class), any(Pageable.class));
        }

        @Test
        @DisplayName("Should handle sorting correctly")
        void shouldHandleSortingCorrectly() {
            // Given
            filterRequest.setSortBy("email");
            filterRequest.setSortDir("DESC");
            
            Page<User> expectedPage = new PageImpl<>(testUsers);
            when(userRepository.findAll(any(Specification.class), any(Pageable.class)))
                    .thenReturn(expectedPage);

            // When
            Page<User> result = userSearchService.searchUsers(filterRequest);

            // Then
            assertNotNull(result);
            verify(userRepository).findAll(any(Specification.class), any(Pageable.class));
        }

        @Test
        @DisplayName("Should handle null and empty filter values")
        void shouldHandleNullAndEmptyFilterValues() {
            // Given
            filterRequest.setName(null);
            filterRequest.setEmail("");
            filterRequest.setRole(null);
            filterRequest.setEnabled(null);
            
            Page<User> expectedPage = new PageImpl<>(testUsers);
            when(userRepository.findAll(any(Specification.class), any(Pageable.class)))
                    .thenReturn(expectedPage);

            // When
            Page<User> result = userSearchService.searchUsers(filterRequest);

            // Then
            assertNotNull(result);
            assertEquals(3, result.getContent().size());
            verify(userRepository).findAll(any(Specification.class), any(Pageable.class));
        }

        @Test
        @DisplayName("Should handle whitespace-only filter values")
        void shouldHandleWhitespaceOnlyFilterValues() {
            // Given
            filterRequest.setName("   ");
            filterRequest.setEmail("  \t  ");
            
            Page<User> expectedPage = new PageImpl<>(testUsers);
            when(userRepository.findAll(any(Specification.class), any(Pageable.class)))
                    .thenReturn(expectedPage);

            // When
            Page<User> result = userSearchService.searchUsers(filterRequest);

            // Then
            assertNotNull(result);
            assertEquals(3, result.getContent().size());
            verify(userRepository).findAll(any(Specification.class), any(Pageable.class));
        }
    }

    @Nested
    @DisplayName("Search By Name Or Email Tests")
    class SearchByNameOrEmailTests {

        @Test
        @DisplayName("Should search by name or email successfully")
        void shouldSearchByNameOrEmailSuccessfully() {
            // Given
            String searchTerm = "John";
            when(userRepository.findAll(any(Specification.class))).thenReturn(Arrays.asList(testUsers.get(0)));

            // When
            List<User> result = userSearchService.searchByNameOrEmail(searchTerm);

            // Then
            assertNotNull(result);
            assertEquals(1, result.size());
            assertEquals("John", result.get(0).getFirstName());
            verify(userRepository).findAll(any(Specification.class));
        }

        @Test
        @DisplayName("Should return empty list for null search term")
        void shouldReturnEmptyListForNullSearchTerm() {
            // Given
            String searchTerm = null;

            // When
            List<User> result = userSearchService.searchByNameOrEmail(searchTerm);

            // Then
            assertNotNull(result);
            assertEquals(0, result.size());
            verify(userRepository, never()).findAll(any(Specification.class));
        }

        @Test
        @DisplayName("Should return empty list for empty search term")
        void shouldReturnEmptyListForEmptySearchTerm() {
            // Given
            String searchTerm = "";

            // When
            List<User> result = userSearchService.searchByNameOrEmail(searchTerm);

            // Then
            assertNotNull(result);
            assertEquals(0, result.size());
            verify(userRepository, never()).findAll(any(Specification.class));
        }

        @Test
        @DisplayName("Should return empty list for whitespace-only search term")
        void shouldReturnEmptyListForWhitespaceOnlySearchTerm() {
            // Given
            String searchTerm = "   ";

            // When
            List<User> result = userSearchService.searchByNameOrEmail(searchTerm);

            // Then
            assertNotNull(result);
            assertEquals(0, result.size());
            verify(userRepository, never()).findAll(any(Specification.class));
        }

        @Test
        @DisplayName("Should search by email successfully")
        void shouldSearchByEmailSuccessfully() {
            // Given
            String searchTerm = "smith";
            when(userRepository.findAll(any(Specification.class))).thenReturn(Arrays.asList(testUsers.get(1)));

            // When
            List<User> result = userSearchService.searchByNameOrEmail(searchTerm);

            // Then
            assertNotNull(result);
            assertEquals(1, result.size());
            assertTrue(result.get(0).getEmail().contains("smith"));
            verify(userRepository).findAll(any(Specification.class));
        }

        @Test
        @DisplayName("Should return multiple matching users")
        void shouldReturnMultipleMatchingUsers() {
            // Given
            String searchTerm = "o"; // Should match John and Bob
            when(userRepository.findAll(any(Specification.class)))
                    .thenReturn(Arrays.asList(testUsers.get(0), testUsers.get(2)));

            // When
            List<User> result = userSearchService.searchByNameOrEmail(searchTerm);

            // Then
            assertNotNull(result);
            assertEquals(2, result.size());
            verify(userRepository).findAll(any(Specification.class));
        }
    }

    @Nested
    @DisplayName("Find By Role Tests")
    class FindByRoleTests {

        @Test
        @DisplayName("Should find users by role successfully")
        void shouldFindUsersByRoleSuccessfully() {
            // Given
            Role role = Role.FREELANCER;
            when(userRepository.findByRole(role)).thenReturn(Arrays.asList(testUsers.get(0)));

            // When
            List<User> result = userSearchService.findByRole(role);

            // Then
            assertNotNull(result);
            assertEquals(1, result.size());
            assertEquals(Role.FREELANCER, result.get(0).getRole());
            verify(userRepository).findByRole(role);
        }

        @Test
        @DisplayName("Should return empty list when no users found for role")
        void shouldReturnEmptyListWhenNoUsersFoundForRole() {
            // Given
            Role role = Role.ADMIN;
            when(userRepository.findByRole(role)).thenReturn(Collections.emptyList());

            // When
            List<User> result = userSearchService.findByRole(role);

            // Then
            assertNotNull(result);
            assertEquals(0, result.size());
            verify(userRepository).findByRole(role);
        }

        @Test
        @DisplayName("Should find multiple users with same role")
        void shouldFindMultipleUsersWithSameRole() {
            // Given
            Role role = Role.FREELANCER;
            User anotherFreelancer = User.builder()
                    .id(4L)
                    .firstName("Alice")
                    .lastName("Brown")
                    .email("alice.brown@example.com")
                    .role(Role.FREELANCER)
                    .enabled(true)
                    .build();
            
            when(userRepository.findByRole(role))
                    .thenReturn(Arrays.asList(testUsers.get(0), anotherFreelancer));

            // When
            List<User> result = userSearchService.findByRole(role);

            // Then
            assertNotNull(result);
            assertEquals(2, result.size());
            assertTrue(result.stream().allMatch(user -> user.getRole() == Role.FREELANCER));
            verify(userRepository).findByRole(role);
        }
    }

    @Nested
    @DisplayName("Find Active/Inactive Users Tests")
    class FindActiveInactiveUsersTests {

        @Test
        @DisplayName("Should find active users successfully")
        void shouldFindActiveUsersSuccessfully() {
            // Given
            when(userRepository.findByEnabled(true))
                    .thenReturn(Arrays.asList(testUsers.get(0), testUsers.get(2)));

            // When
            List<User> result = userSearchService.findActiveUsers();

            // Then
            assertNotNull(result);
            assertEquals(2, result.size());
            assertTrue(result.stream().allMatch(User::isEnabled));
            verify(userRepository).findByEnabled(true);
        }

        @Test
        @DisplayName("Should find inactive users successfully")
        void shouldFindInactiveUsersSuccessfully() {
            // Given
            when(userRepository.findByEnabled(false)).thenReturn(Arrays.asList(testUsers.get(1)));

            // When
            List<User> result = userSearchService.findInactiveUsers();

            // Then
            assertNotNull(result);
            assertEquals(1, result.size());
            assertFalse(result.get(0).isEnabled());
            verify(userRepository).findByEnabled(false);
        }

        @Test
        @DisplayName("Should return empty list when no active users found")
        void shouldReturnEmptyListWhenNoActiveUsersFound() {
            // Given
            when(userRepository.findByEnabled(true)).thenReturn(Collections.emptyList());

            // When
            List<User> result = userSearchService.findActiveUsers();

            // Then
            assertNotNull(result);
            assertEquals(0, result.size());
            verify(userRepository).findByEnabled(true);
        }

        @Test
        @DisplayName("Should return empty list when no inactive users found")
        void shouldReturnEmptyListWhenNoInactiveUsersFound() {
            // Given
            when(userRepository.findByEnabled(false)).thenReturn(Collections.emptyList());

            // When
            List<User> result = userSearchService.findInactiveUsers();

            // Then
            assertNotNull(result);
            assertEquals(0, result.size());
            verify(userRepository).findByEnabled(false);
        }
    }

    @Nested
    @DisplayName("Edge Cases Tests")
    class EdgeCasesTests {

        @Test
        @DisplayName("Should handle repository exception in search")
        void shouldHandleRepositoryExceptionInSearch() {
            // Given
            when(userRepository.findAll(any(Specification.class), any(Pageable.class)))
                    .thenThrow(new RuntimeException("Database error"));

            // When & Then
            assertThrows(RuntimeException.class, () -> userSearchService.searchUsers(filterRequest));
            verify(userRepository).findAll(any(Specification.class), any(Pageable.class));
        }

        @Test
        @DisplayName("Should handle repository exception in findByRole")
        void shouldHandleRepositoryExceptionInFindByRole() {
            // Given
            when(userRepository.findByRole(any(Role.class)))
                    .thenThrow(new RuntimeException("Database error"));

            // When & Then
            assertThrows(RuntimeException.class, () -> userSearchService.findByRole(Role.ADMIN));
            verify(userRepository).findByRole(Role.ADMIN);
        }

        @Test
        @DisplayName("Should handle special characters in search terms")
        void shouldHandleSpecialCharactersInSearchTerms() {
            // Given
            String searchTerm = "john@doe.com";
            when(userRepository.findAll(any(Specification.class))).thenReturn(Collections.emptyList());

            // When
            List<User> result = userSearchService.searchByNameOrEmail(searchTerm);

            // Then
            assertNotNull(result);
            assertEquals(0, result.size());
            verify(userRepository).findAll(any(Specification.class));
        }

        @Test
        @DisplayName("Should handle very long search terms")
        void shouldHandleVeryLongSearchTerms() {
            // Given
            String longSearchTerm = "a".repeat(1000);
            when(userRepository.findAll(any(Specification.class))).thenReturn(Collections.emptyList());

            // When
            List<User> result = userSearchService.searchByNameOrEmail(longSearchTerm);

            // Then
            assertNotNull(result);
            assertEquals(0, result.size());
            verify(userRepository).findAll(any(Specification.class));
        }

        @Test
        @DisplayName("Should handle case-insensitive search")
        void shouldHandleCaseInsensitiveSearch() {
            // Given
            String searchTerm = "JOHN";
            when(userRepository.findAll(any(Specification.class))).thenReturn(Arrays.asList(testUsers.get(0)));

            // When
            List<User> result = userSearchService.searchByNameOrEmail(searchTerm);

            // Then
            assertNotNull(result);
            assertEquals(1, result.size());
            verify(userRepository).findAll(any(Specification.class));
        }
    }
}