package Controller;

import Entity.User;
import Service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final UserService userService;

    public AdminController(UserService userService) {
        this.userService = userService;
    }

    // ─── GET ALL USERS ───────────────────────────────────────────
    @GetMapping("/users")
    public ResponseEntity<List<User>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    // ─── DELETE USER ─────────────────────────────────────────────
    @DeleteMapping("/users/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable Long id) {
        try {
            userService.deleteUser(id);
            return ResponseEntity.ok(java.util.Map.of(
                "message", "Utilisateur supprimé avec succès",
                "success", true
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(java.util.Map.of("error", e.getMessage()));
        }
    }

    // ─── UPDATE USER STATUS ───────────────────────────────────────
    @PatchMapping("/users/{id}/status")
    public ResponseEntity<?> updateUserStatus(@PathVariable Long id, @RequestBody java.util.Map<String, Boolean> request) {
        try {
            Boolean enabled = request.get("enabled");
            if (enabled == null) {
                return ResponseEntity.badRequest().body(java.util.Map.of("error", "Enabled status is required"));
            }
            
            userService.updateUserStatus(id, enabled);
            String status = enabled ? "activé" : "désactivé";
            return ResponseEntity.ok(java.util.Map.of(
                "message", "Utilisateur " + status + " avec succès",
                "success", true,
                "enabled", enabled
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(java.util.Map.of("error", e.getMessage()));
        }
    }
}