package Controller;

import DTO.LegacyAuthResponse;
import DTO.LegacyProfileResponse;
import DTO.LegacyRegisterRequest;
import DTO.LegacyUserSummaryResponse;
import DTO.SignInRequest;
import Entity.Role;
import Entity.User;
import Repository.UserRepository;
import Security.JwtUtils;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * API attendue par le frontend MatchFreelance (Angular) via la gateway /api/users.
 */
@RestController
@RequestMapping("/api/users")
public class LegacyUsersController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtils jwtUtils;

    public LegacyUsersController(UserRepository userRepository, PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager, JwtUtils jwtUtils) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtUtils = jwtUtils;
    }

    private static String apiRoleFromEntity(Role r) {
        if (r == Role.PROJECT_OWNER) {
            return "CLIENT";
        }
        return r.name();
    }

    private static Role roleFromApi(String apiRole) {
        if (apiRole == null || apiRole.isBlank()) {
            throw new IllegalArgumentException("Le rôle est requis");
        }
        switch (apiRole.trim().toUpperCase()) {
            case "CLIENT":
                return Role.PROJECT_OWNER;
            case "FREELANCER":
                return Role.FREELANCER;
            case "ADMIN":
                return Role.ADMIN;
            default:
                throw new IllegalArgumentException("Rôle inconnu : " + apiRole);
        }
    }

    private static String fullNameOf(User u) {
        String fn = u.getFirstName() != null ? u.getFirstName() : "";
        String ln = u.getLastName() != null ? u.getLastName() : "";
        String t = (fn + " " + ln).trim();
        return t.isEmpty() ? null : t;
    }

    private static String[] splitFullName(String fullName) {
        if (fullName == null || fullName.isBlank()) {
            return new String[] { "Utilisateur", "MatchFreelance" };
        }
        String t = fullName.trim();
        int sp = t.indexOf(' ');
        if (sp < 0) {
            return new String[] { t, "-" };
        }
        String last = t.substring(sp + 1).trim();
        if (last.isEmpty()) {
            last = "-";
        }
        return new String[] { t.substring(0, sp), last };
    }

    /** Evite le WARN si on ouvre l’URL dans le navigateur (GET) au lieu d’un POST JSON. */
    @GetMapping("/auth/login")
    public ResponseEntity<Map<String, Object>> legacyLoginHelp() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("message", "Cette URL attend une requete POST (JSON), pas un GET dans le navigateur.");
        body.put("method", "POST");
        body.put("contentType", "application/json");
        body.put("exemple", Map.of("email", "admin@app.com", "password", "votre_mot_de_passe"));
        body.put("frontend", "Utilisez http://localhost:4200/login (Angular envoie le POST automatiquement).");
        return ResponseEntity.ok(body);
    }

    @PostMapping("/auth/login")
    public ResponseEntity<?> legacyLogin(@Valid @RequestBody SignInRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));
            User user = userRepository.findByEmail(request.getEmail())
                    .orElseThrow(() -> new BadCredentialsException("Utilisateur introuvable"));
            String token = jwtUtils.generateToken(user.getEmail(), user.getRole().name());
            return ResponseEntity.ok(new LegacyAuthResponse(token, user.getId(), user.getEmail(),
                    fullNameOf(user), apiRoleFromEntity(user.getRole())));
        } catch (BadCredentialsException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Identifiants invalides");
        } catch (DisabledException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Compte désactivé");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Connexion impossible");
        }
    }

    @GetMapping("/auth/register")
    public ResponseEntity<Map<String, Object>> legacyRegisterHelp() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("message", "Cette URL attend une requete POST (JSON), pas un GET dans le navigateur.");
        body.put("method", "POST");
        body.put("contentType", "application/json");
        body.put("exemple", Map.of(
                "email", "nouveau@example.com",
                "password", "Secret123!",
                "role", "FREELANCER ou CLIENT",
                "fullName", "Optionnel"));
        body.put("frontend", "Utilisez http://localhost:4200/register");
        return ResponseEntity.ok(body);
    }

    @PostMapping("/auth/register")
    public ResponseEntity<?> legacyRegister(@Valid @RequestBody LegacyRegisterRequest request) {
        try {
            if (userRepository.existsByEmail(request.getEmail())) {
                return ResponseEntity.badRequest().body("Cet email est déjà utilisé");
            }
            Role role = roleFromApi(request.getRole());
            if (role == Role.ADMIN) {
                return ResponseEntity.badRequest()
                        .body("Impossible de créer un compte administrateur via cette API");
            }
            String[] names = splitFullName(request.getFullName());
            User user = User.builder()
                    .firstName(names[0])
                    .lastName(names[1])
                    .address("N/A")
                    .email(request.getEmail())
                    .password(passwordEncoder.encode(request.getPassword()))
                    .birthDate(LocalDate.of(2000, 1, 1))
                    .role(role)
                    .enabled(true)
                    .build();
            user = userRepository.save(user);
            String token = jwtUtils.generateToken(user.getEmail(), user.getRole().name());
            return ResponseEntity.ok(new LegacyAuthResponse(token, user.getId(), user.getEmail(),
                    fullNameOf(user), apiRoleFromEntity(user.getRole())));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Erreur : " + e.getMessage());
        }
    }

    @GetMapping("/me/profile")
    public ResponseEntity<?> legacyProfile() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        User user = userRepository.findByEmail(auth.getName()).orElse(null);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        String name = fullNameOf(user);
        return ResponseEntity.ok(new LegacyProfileResponse(user.getId(), user.getEmail(),
                name != null ? name : "", apiRoleFromEntity(user.getRole())));
    }

    @GetMapping
    public ResponseEntity<?> listUsers(@RequestParam(required = false) String role) {
        List<User> users;
        if (role != null && !role.isBlank()) {
            Role filter;
            try {
                filter = roleFromApi(role);
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().body(e.getMessage());
            }
            users = userRepository.findByRole(filter);
        } else {
            users = userRepository.findAll();
        }
        List<LegacyUserSummaryResponse> out = users.stream()
                .map(u -> new LegacyUserSummaryResponse(u.getId(), u.getEmail(), fullNameOf(u),
                        apiRoleFromEntity(u.getRole())))
                .collect(Collectors.toList());
        return ResponseEntity.ok(out);
    }

    @DeleteMapping("/me")
    public ResponseEntity<Void> deleteMyAccount() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        User user = userRepository.findByEmail(auth.getName()).orElse(null);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        userRepository.delete(user);
        return ResponseEntity.noContent().build();
    }
}
