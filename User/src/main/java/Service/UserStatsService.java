package Service;

import DTO.UserStatsResponse;
import Entity.Role;
import Entity.User;
import Repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class UserStatsService {

    private final UserRepository userRepository;

    public UserStatsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public UserStatsResponse getUserStatistics() {
        List<User> allUsers = userRepository.findAll();
        
        // Calculate statistics
        long totalUsers = allUsers.size();
        long activeFreelancers = allUsers.stream()
                .filter(user -> user.getRole() == Role.FREELANCER && user.isEnabled())
                .count();
        long activeProjectOwners = allUsers.stream()
                .filter(user -> user.getRole() == Role.PROJECT_OWNER && user.isEnabled())
                .count();
        long activeAccounts = allUsers.stream()
                .filter(User::isEnabled)
                .count();
        long inactiveAccounts = allUsers.stream()
                .filter(user -> !user.isEnabled())
                .count();
        long totalAdmins = allUsers.stream()
                .filter(user -> user.getRole() == Role.ADMIN)
                .count();

        return new UserStatsResponse(
                totalUsers,
                activeFreelancers,
                activeProjectOwners,
                activeAccounts,
                inactiveAccounts,
                totalAdmins
        );
    }

    public UserStatsResponse getStatisticsByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        List<User> usersInRange = userRepository.findAll().stream()
                .filter(user -> {
                    // Assuming you have a creation date field, if not, use registration date
                    return true; // Placeholder - adjust based on your actual User entity
                })
                .toList();

        long activeFreelancers = usersInRange.stream()
                .filter(user -> user.getRole() == Role.FREELANCER && user.isEnabled())
                .count();
        long activeProjectOwners = usersInRange.stream()
                .filter(user -> user.getRole() == Role.PROJECT_OWNER && user.isEnabled())
                .count();
        long activeAccounts = usersInRange.stream()
                .filter(User::isEnabled)
                .count();
        long inactiveAccounts = usersInRange.stream()
                .filter(user -> !user.isEnabled())
                .count();
        long totalAdmins = usersInRange.stream()
                .filter(user -> user.getRole() == Role.ADMIN)
                .count();

        return new UserStatsResponse(
                usersInRange.size(),
                activeFreelancers,
                activeProjectOwners,
                activeAccounts,
                inactiveAccounts,
                totalAdmins
        );
    }
}
