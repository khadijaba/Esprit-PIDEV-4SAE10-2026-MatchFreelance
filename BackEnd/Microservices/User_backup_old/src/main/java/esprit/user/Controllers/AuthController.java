package esprit.user.Controllers;

import esprit.user.dto.AuthResponse;
import esprit.user.dto.LoginRequest;
import esprit.user.dto.RegisterRequest;
import esprit.user.dto.UpdateUserRequest;
import esprit.user.entities.User;
import esprit.user.entities.UserRole;
import esprit.user.Service.AuthService;
import esprit.user.security.LocalUserPrincipal;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Auth locale : JWT signé par le service (secret {@code jwt.secret}), mot de passe BCrypt.
 */
@RestController
@RequestMapping("/users")
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        try {
            return ResponseEntity.ok(authService.login(request));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> me(@AuthenticationPrincipal LocalUserPrincipal principal) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("subject", String.valueOf(principal.id()));
        body.put("email", principal.email());
        body.put("localUserId", principal.id());
        body.put("localRole", principal.role());
        authService.findByIdForPrincipal(principal.id()).ifPresent(u -> {
            body.put("fullName", u.getFullName());
        });
        return ResponseEntity.ok(body);
    }

    @GetMapping
    public ResponseEntity<List<User>> listUsers(
            @RequestParam(required = false) UserRole role) {
        List<User> users = role != null ? authService.findByRole(role) : authService.findAll();
        users.forEach(u -> u.setPassword(null));
        return ResponseEntity.ok(users);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getUserById(@PathVariable Long id) {
        return authService.findById(id)
                .map(user -> {
                    user.setPassword(null);
                    return ResponseEntity.ok(user);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateUser(@PathVariable Long id, @RequestBody UpdateUserRequest request) {
        try {
            User user = authService.update(id, request.getFullName());
            user.setPassword(null);
            return ResponseEntity.ok(user);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
