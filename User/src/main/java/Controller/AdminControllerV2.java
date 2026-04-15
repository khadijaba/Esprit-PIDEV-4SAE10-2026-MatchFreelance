package Controller;

import DTO.PageResponse;
import DTO.UserFilterRequest;
import DTO.UserStatsResponse;
import Entity.User;
import Service.UserSearchService;
import Service.UserStatsService;
import Service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/v2")
@PreAuthorize("hasRole('ADMIN')")
public class AdminControllerV2 {

    private final UserService userService;
    private final UserSearchService userSearchService;
    private final UserStatsService userStatsService;

    @Autowired
    public AdminControllerV2(UserService userService, UserSearchService userSearchService, UserStatsService userStatsService) {
        this.userService = userService;
        this.userSearchService = userSearchService;
        this.userStatsService = userStatsService;
    }

    // ─── STATISTICS ────────────────────────────────────────────────
    @GetMapping("/statistics")
    public ResponseEntity<UserStatsResponse> getUserStatistics() {
        UserStatsResponse stats = userStatsService.getUserStatistics();
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/statistics/range")
    public ResponseEntity<UserStatsResponse> getStatisticsByDateRange(
            @RequestParam String startDate,
            @RequestParam String endDate) {
        
        // Parse dates - in production, add proper date validation
        java.time.LocalDateTime start = java.time.LocalDateTime.parse(startDate);
        java.time.LocalDateTime end = java.time.LocalDateTime.parse(endDate);
        
        UserStatsResponse stats = userStatsService.getStatisticsByDateRange(start, end);
        return ResponseEntity.ok(stats);
    }

    // ─── ADVANCED USER SEARCH ────────────────────────────────────────
    @PostMapping("/search")
    public ResponseEntity<PageResponse<User>> searchUsers(@RequestBody UserFilterRequest filter) {
        Page<User> users = userSearchService.searchUsers(filter);
        
        PageResponse<User> response = new PageResponse<>(
                users.getContent(),
                users.getNumber(),
                users.getSize(),
                users.getTotalElements()
        );
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/search/simple")
    public ResponseEntity<?> searchByNameOrEmail(@RequestParam String term) {
        var users = userSearchService.searchByNameOrEmail(term);
        return ResponseEntity.ok(users);
    }

    // ─── FILTERED USER LISTS ────────────────────────────────────────
    @GetMapping("/users/role/{role}")
    public ResponseEntity<?> getUsersByRole(@PathVariable Entity.Role role) {
        var users = userSearchService.findByRole(role);
        return ResponseEntity.ok(users);
    }

    @GetMapping("/users/active")
    public ResponseEntity<?> getActiveUsers() {
        var users = userSearchService.findActiveUsers();
        return ResponseEntity.ok(users);
    }

    @GetMapping("/users/inactive")
    public ResponseEntity<?> getInactiveUsers() {
        var users = userSearchService.findInactiveUsers();
        return ResponseEntity.ok(users);
    }

    // ─── LEGACY ENDPOINTS (compatibility) ────────────────────────
    @GetMapping("/users")
    public ResponseEntity<PageResponse<User>> getAllUsersPaginated(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {
        
        UserFilterRequest filter = new UserFilterRequest();
        filter.setPage(page);
        filter.setSize(size);
        filter.setSortBy(sortBy);
        filter.setSortDir(sortDir);
        
        Page<User> users = userSearchService.searchUsers(filter);
        
        PageResponse<User> response = new PageResponse<>(
                users.getContent(),
                users.getNumber(),
                users.getSize(),
                users.getTotalElements()
        );
        
        return ResponseEntity.ok(response);
    }

    // ─── USER MANAGEMENT (existing functionality) ────────────────────
    @DeleteMapping("/users/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable Long id) {
        try {
            userService.deleteUser(id);
            return ResponseEntity.ok("Utilisateur supprimé avec succès");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
