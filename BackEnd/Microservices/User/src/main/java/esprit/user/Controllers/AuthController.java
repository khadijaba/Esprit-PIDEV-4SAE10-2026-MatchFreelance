package esprit.user.Controllers;

import esprit.user.dto.InitialRoleRequest;
import esprit.user.dto.UpdateUserRequest;
import esprit.user.entities.User;
import esprit.user.entities.UserRole;
import esprit.user.Service.AuthService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Sécurité Keycloak : validation du JWT (issuer + signature), sans introspection.
 */
@RestController
@RequestMapping("/users")
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/profile/sync")
    public ResponseEntity<User> syncProfile(@AuthenticationPrincipal Jwt jwt) {
        if (jwt == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        User user = authService.syncProfileFromKeycloak(jwt);
        user.setPassword(null);
        return ResponseEntity.ok(user);
    }

    @PostMapping("/profile/initial-role")
    public ResponseEntity<User> initialRole(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody InitialRoleRequest body) {
        if (jwt == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        if (body == null || body.role() == null) {
            return ResponseEntity.badRequest().build();
        }
        try {
            User user = authService.applyInitialRole(jwt, body.role());
            user.setPassword(null);
            return ResponseEntity.ok(user);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
    }

    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> me(@AuthenticationPrincipal JwtAuthenticationToken token) {
        if (token == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        Jwt jwt = token.getToken();
        String email = jwt.getClaimAsString("email");
        if (email == null || email.isBlank()) {
            email = jwt.getClaimAsString("preferred_username");
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("subject", jwt.getSubject());
        body.put("preferred_username", jwt.getClaimAsString("preferred_username"));
        body.put("email", jwt.getClaimAsString("email"));
        body.put("name", jwt.getClaimAsString("name"));
        body.put("realm_access", jwt.getClaimAsMap("realm_access"));
        body.put("authorities", token.getAuthorities().stream()
                .map(a -> a.getAuthority())
                .collect(Collectors.toList()));
        authService.findByEmailForMe(email != null ? email : "").ifPresent(u -> {
            body.put("localUserId", u.getId());
            body.put("localRole", u.getRole());
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
