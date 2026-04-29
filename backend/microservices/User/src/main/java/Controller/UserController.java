package Controller;

import DTO.ApiLoginResponse;
import DTO.SignInRequest;
import Entity.Role;
import Entity.User;
import Repository.UserRepository;
import Security.JwtUtils;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserRepository userRepository;
    private final AuthenticationManager authenticationManager;
    private final JwtUtils jwtUtils;

    public UserController(
            UserRepository userRepository,
            AuthenticationManager authenticationManager,
            JwtUtils jwtUtils) {
        this.userRepository = userRepository;
        this.authenticationManager = authenticationManager;
        this.jwtUtils = jwtUtils;
    }

    /**
     * Connexion utilisée par le frontend ({@code POST /api/users/login}).
     * Le flux historique reste {@code POST /api/auth/signin}.
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody SignInRequest request) {
        try {
            String emailIn = request.getEmail() != null ? request.getEmail().trim() : "";
            String password = request.getPassword() != null ? request.getPassword() : "";
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(emailIn, password));
            String email = authentication.getName();
            String role = authentication.getAuthorities().iterator().next().getAuthority()
                    .replace("ROLE_", "");

            User user = userRepository.findByEmailIgnoreCase(email)
                    .orElseThrow(() -> new BadCredentialsException("Utilisateur introuvable"));

            String token = jwtUtils.generateToken(email, role);
            String fullName = (user.getFirstName() + " " + user.getLastName()).trim();

            return ResponseEntity.ok(new ApiLoginResponse(
                    token,
                    user.getId(),
                    email,
                    fullName,
                    role));

        } catch (BadCredentialsException e) {
            return ResponseEntity.status(401).body("Email ou mot de passe incorrect");
        } catch (DisabledException e) {
            return ResponseEntity.status(403)
                    .body("Ce compte est désactivé. Veuillez vérifier votre email pour activer votre compte.");
        }
    }
    @Value("${welcome.message:Bienvenue dans le microservice USER}")
    private String welcomeMessage;
    @GetMapping("/welcome")
    public String welcome() {
        return welcomeMessage;
    }

    /**
     * Liste des utilisateurs (entités {@link User}, mots de passe masqués).
     * <p>La racine {@code GET /api/users} est servie par {@link LegacyUsersController} (format
     * MatchFreelance : rôle CLIENT, etc.) pour éviter un mapping en double.
     * <p>{@code GET /api/users/all} reste un alias utile pour les gateways / appels qui préfèrent un segment explicite.
     */
    @GetMapping("/all")
    public ResponseEntity<?> listUsersAll(@RequestParam(required = false) String role) {
        return listUsersInternal(role);
    }

    private ResponseEntity<?> listUsersInternal(String role) {
        try {
            List<User> users;
            if (role != null && !role.isBlank()) {
                Role r = Role.fromSignupString(role);
                if (r == Role.PROJECT_OWNER) {
                    users = userRepository.findByUserRoleIn(List.of(Role.PROJECT_OWNER, Role.CLIENT));
                } else {
                    users = userRepository.findByUserRole(r);
                }
            } else {
                users = userRepository.findAll();
            }
            users.forEach(u -> u.setPassword(null));
            return ResponseEntity.ok(users);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Rôle inconnu. Utilisez FREELANCER, PROJECT_OWNER ou ADMIN.");
        }
    }

    // Get the currently authenticated user
    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth.getName().equals("anonymousUser")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not authenticated");
        }

        String email = auth.getName();
        User user = userRepository.findByEmail(email)
                .orElse(null);

        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found");
        }

        // Return user (password should ideally be stripped in a DTO, but for simplicity
        // here we return it directly,
        // Jackson will serialize the entity. Make sure you don't expose password if
        // User.java doesn't have @JsonIgnore on it.
        // Wait, User.java doesn't have @JsonIgnore on password. We must clear it!)
        user.setPassword(null);
        return ResponseEntity.ok(user);
    }

    /**
     * Profil par identifiant (appels inter-services : Candidature, etc.).
     * Ne doit pas capturer {@code /me} ({@code id} numérique uniquement).
     */
    @GetMapping("/{id:\\d+}")
    public ResponseEntity<User> getUserById(@PathVariable Long id) {
               Optional<User> found = userRepository.findById(id);
        if (found.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).<User>body(null);
        }
        User u = found.get();
        u.setPassword(null);
        return ResponseEntity.ok(u);
    }

    @PutMapping("/me")
    public ResponseEntity<?> updateProfile(@RequestBody User updatedUser) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated() || auth.getName().equals("anonymousUser")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not authenticated");
            }

            String email = auth.getName();
            User user = userRepository.findByEmail(email).orElse(null);
            if (user == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found");
            }

            // Update allowed fields
            if (updatedUser.getFirstName() != null)
                user.setFirstName(updatedUser.getFirstName());
            if (updatedUser.getLastName() != null)
                user.setLastName(updatedUser.getLastName());
            if (updatedUser.getAddress() != null)
                user.setAddress(updatedUser.getAddress());
            if (updatedUser.getBirthDate() != null)
                user.setBirthDate(updatedUser.getBirthDate());
            // Role and Password should NOT be updated here for security reasons
            // (Password has its own flow)

            userRepository.save(user);
            user.setPassword(null); // Strip password for response
            return ResponseEntity.ok(user);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error updating profile: " + e.getMessage());
        }
    }

    @PostMapping("/me/avatar")
    public ResponseEntity<?> uploadAvatar(@RequestParam("file") MultipartFile file) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated() || auth.getName().equals("anonymousUser")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not authenticated");
            }

            String email = auth.getName();
            User user = userRepository.findByEmail(email).orElse(null);
            if (user == null)
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found");

            // Define the upload directory
            Path uploadDir = Paths.get("uploads", "avatars");
            if (!Files.exists(uploadDir)) {
                Files.createDirectories(uploadDir);
            }

            // Generate unique filename to avoid overriding other users
            String originalFilename = StringUtils.cleanPath(file.getOriginalFilename());
            String extension = "";
            int i = originalFilename.lastIndexOf('.');
            if (i > 0) {
                extension = originalFilename.substring(i);
            }
            String newFilename = "user_" + user.getId() + "_" + UUID.randomUUID().toString() + extension;
            Path targetLocation = uploadDir.resolve(newFilename);

            // Save file
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            // Construct the URL exactly as mapped by WebMvcConfig
            String fileUrl = "/uploads/avatars/" + newFilename;

            // Delete old avatar if it exists (Optional, but good for saving disk space)
            if (user.getProfilePictureUrl() != null && user.getProfilePictureUrl().startsWith("/uploads/avatars/")) {
                try {
                    String oldFileName = user.getProfilePictureUrl().replace("/uploads/avatars/", "");
                    Path oldFilePath = uploadDir.resolve(oldFileName);
                    Files.deleteIfExists(oldFilePath);
                } catch (Exception e) {
                    System.out.println("Could not delete old avatar: " + e.getMessage());
                }
            }

            // Update user entity
            user.setProfilePictureUrl(fileUrl);
            userRepository.save(user);

            return ResponseEntity.ok().body("{\"url\": \"" + fileUrl + "\"}");

        } catch (IOException ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Could not store file. Please try again.");
        }
    }
}
