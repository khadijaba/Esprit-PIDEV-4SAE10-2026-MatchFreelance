package esprit.user.security;

import esprit.user.entities.UserRole;
import org.springframework.security.core.AuthenticatedPrincipal;

public record LocalUserPrincipal(Long id, String email, UserRole role) implements AuthenticatedPrincipal {

    @Override
    public String getName() {
        return email != null ? email : String.valueOf(id);
    }
}
