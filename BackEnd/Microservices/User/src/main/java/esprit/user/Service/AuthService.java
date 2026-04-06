package esprit.user.Service;

import esprit.user.entities.User;
import esprit.user.entities.UserRole;
import esprit.user.Repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Synchronise le profil local à partir des claims du JWT Keycloak.
     */
    @Transactional
    public User syncProfileFromKeycloak(Jwt jwt) {
        String email = resolveEmail(jwt);
        UserRole role = resolveRole(jwt);

        Optional<User> existing = userRepository.findByEmail(email);
        if (existing.isPresent()) {
            User u = existing.get();
            // Ne pas écraser FREELANCER/CLIENT depuis le JWT : le rôle métier vient de l’app (inscription) ou de l’admin.
            String name = jwt.getClaimAsString("name");
            if (name != null && !name.isBlank()) {
                u.setFullName(name.trim());
            }
            return userRepository.save(u);
        }

        User user = new User();
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
        // Nouveau compte : CLIENT par défaut ; l’utilisateur choisit FREELANCER ou CLIENT via /profile/initial-role.
        // Seul ADMIN issu du realm est appliqué tout de suite.
        if (role == UserRole.ADMIN) {
            user.setRole(UserRole.ADMIN);
        } else {
            user.setRole(UserRole.CLIENT);
        }
        String name = jwt.getClaimAsString("name");
        String given = jwt.getClaimAsString("given_name");
        String family = jwt.getClaimAsString("family_name");
        if (name != null && !name.isBlank()) {
            user.setFullName(name.trim());
        } else if (given != null || family != null) {
            user.setFullName((given != null ? given : "").trim() + " " + (family != null ? family : "").trim());
        }
        return userRepository.save(user);
    }

    private static String resolveEmail(Jwt jwt) {
        String email = jwt.getClaimAsString("email");
        if (email != null && !email.isBlank()) {
            return email.trim().toLowerCase();
        }
        String preferred = jwt.getClaimAsString("preferred_username");
        if (preferred != null && !preferred.isBlank()) {
            return preferred.trim().toLowerCase();
        }
        String sub = jwt.getSubject();
        return (sub != null ? sub : "user") + "@keycloak.local";
    }

    private static UserRole resolveRole(Jwt jwt) {
        Map<String, Object> realm = jwt.getClaimAsMap("realm_access");
        if (realm == null || !(realm.get("roles") instanceof List<?> roles)) {
            return null;
        }
        for (Object r : roles) {
            if (r == null) {
                continue;
            }
            try {
                return UserRole.valueOf(r.toString().toUpperCase());
            } catch (IllegalArgumentException ignored) {
                // ignore
            }
        }
        return null;
    }

    public List<User> findAll() {
        return userRepository.findAll();
    }

    public List<User> findByRole(UserRole role) {
        return userRepository.findByRole(role);
    }

    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    @Transactional
    public User update(Long id, String fullName) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        if (fullName != null) {
            user.setFullName(fullName.trim().isEmpty() ? null : fullName.trim());
        }
        return userRepository.save(user);
    }

    public Optional<User> findByEmailForMe(String email) {
        if (email == null || email.isBlank()) {
            return Optional.empty();
        }
        return userRepository.findByEmail(email.trim().toLowerCase());
    }

    /**
     * Une fois après inscription : FREELANCER ou CLIENT uniquement, tant que le rôle local est encore CLIENT par défaut.
     */
    @Transactional
    public User applyInitialRole(Jwt jwt, UserRole requested) {
        if (requested != UserRole.FREELANCER && requested != UserRole.CLIENT) {
            throw new IllegalArgumentException("Only FREELANCER or CLIENT allowed");
        }
        String email = resolveEmail(jwt);
        User u = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        if (u.getRole() != UserRole.CLIENT) {
            throw new IllegalStateException("Initial role already chosen or account is not pending");
        }
        u.setRole(requested);
        return userRepository.save(u);
    }
}
