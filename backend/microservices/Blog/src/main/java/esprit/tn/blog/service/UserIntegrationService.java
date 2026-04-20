package esprit.tn.blog.service;

import esprit.tn.blog.dto.UserDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.RestClientException;

/**
 * Service to integrate with User microservice (port 9090)
 * Handles fetching user data for blog operations
 */
@Service
@Slf4j
public class UserIntegrationService {

    private final RestTemplate restTemplate;
    private final String userServiceUrl;

    public UserIntegrationService(RestTemplate restTemplate, 
                                @Value("${user.service.url:http://localhost:9090}") String userServiceUrl) {
        this.restTemplate = restTemplate;
        this.userServiceUrl = userServiceUrl;
    }

    /**
     * Fetch user data from User microservice by user ID
     * @param userId User ID from User microservice
     * @return UserDto with user information
     */
    public UserDto getUserById(Long userId) {
        try {
            String url = userServiceUrl + "/api/users/" + userId;
            UserDto user = restTemplate.getForObject(url, UserDto.class);
            
            if (user != null) {
                log.debug("Successfully fetched user data for ID: {}", userId);
                return user;
            } else {
                log.warn("User not found for ID: {}", userId);
                return createFallbackUser(userId);
            }
        } catch (RestClientException e) {
            log.error("Failed to fetch user data for ID: {}. Error: {}", userId, e.getMessage());
            return createFallbackUser(userId);
        }
    }

    /**
     * Populate user display fields for forum posts
     * @param userId User ID from User microservice
     * @return Array with [fullName, email, profilePictureUrl]
     */
    public String[] getUserDisplayData(Long userId) {
        UserDto user = getUserById(userId);
        return new String[]{
            user.getFullName().trim(),
            user.getEmail(),
            user.getProfilePictureUrl()
        };
    }

    /**
     * Get user's full name for display
     * @param userId User ID from User microservice
     * @return Full name (firstName + lastName) or email as fallback
     */
    public String getUserDisplayName(Long userId) {
        UserDto user = getUserById(userId);
        String fullName = user.getFullName().trim();
        return fullName.isEmpty() ? user.getEmail() : fullName;
    }

    /**
     * Get user's profile picture URL
     * @param userId User ID from User microservice
     * @return Profile picture URL or null
     */
    public String getUserAvatar(Long userId) {
        UserDto user = getUserById(userId);
        return user.getProfilePictureUrl();
    }

    /**
     * Check if user exists and is enabled
     * @param userId User ID from User microservice
     * @return true if user exists and is enabled
     */
    public boolean isUserValid(Long userId) {
        UserDto user = getUserById(userId);
        return user != null && user.isEnabled();
    }

    /**
     * Create fallback user data when User microservice is unavailable
     * @param userId User ID
     * @return Basic UserDto with fallback data
     */
    private UserDto createFallbackUser(Long userId) {
        UserDto fallback = new UserDto();
        fallback.setId(userId);
        fallback.setFirstName("User");
        fallback.setLastName(String.valueOf(userId));
        fallback.setEmail("user" + userId + "@example.com");
        fallback.setRole("FREELANCER");
        fallback.setEnabled(true);
        
        log.info("Created fallback user data for ID: {}", userId);
        return fallback;
    }
}