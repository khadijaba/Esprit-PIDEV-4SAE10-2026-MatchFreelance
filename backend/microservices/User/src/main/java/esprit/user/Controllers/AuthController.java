package esprit.user.Controllers;

import esprit.user.dto.AuthResponse;
import esprit.user.dto.LoginRequest;
import esprit.user.dto.RegisterRequest;
import esprit.user.entities.User;
import esprit.user.entities.UserRole;
import esprit.user.Service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/users")
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/auth/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        try {
            AuthResponse response = authService.register(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/auth/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        try {
            AuthResponse response = authService.login(request);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/me")
    public ResponseEntity<?> me(@AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(Map.of(
                "email", userDetails.getUsername(),
                "authorities", userDetails.getAuthorities().stream()
                        .map(a -> a.getAuthority())
                        .collect(Collectors.toList())
        ));
    }

    /** Profil complet du freelancer/utilisateur connecté (userId = id du User dans le microservice User). */
    @GetMapping("/me/profile")
    public ResponseEntity<?> meProfile(
            @AuthenticationPrincipal UserDetails userDetails,
            HttpServletRequest request) {
        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        Long userId = (Long) request.getAttribute("userId");
        String role = (String) request.getAttribute("userRole");
        if (userId == null || role == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return authService.findById(userId)
                .map(u -> {
                    Map<String, Object> profile = new HashMap<>();
                    profile.put("userId", u.getId());
                    profile.put("email", u.getEmail());
                    profile.put("fullName", u.getFullName() != null ? u.getFullName() : "");
                    profile.put("role", u.getRole().name());
                    profile.put("createdAt", u.getCreatedAt());
                    return ResponseEntity.ok(profile);
                })
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    @GetMapping
    public ResponseEntity<List<User>> listUsers(
            @RequestParam(required = false) UserRole role) {
        List<User> users = role != null ? authService.findByRole(role) : authService.findAll();
        users.forEach(u -> u.setPassword(null));
        return ResponseEntity.ok(users);
    }
}
