package Service;


import Entity.Role;
import Entity.User;
import Repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    public UserDetailsServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        if (email == null || email.isBlank()) {
            throw new UsernameNotFoundException("Email vide");
        }
        String key = email.trim();
        User user = userRepository.findByEmailIgnoreCase(key)
                .orElseThrow(() -> new UsernameNotFoundException("Utilisateur non trouvé : " + key));

        Role role = user.getRole();
        if (role == null) {
            throw new UsernameNotFoundException("Rôle utilisateur invalide pour : " + user.getEmail());
        }
        String authority = (role == Role.CLIENT || role == Role.PROJECT_OWNER)
                ? "ROLE_PROJECT_OWNER"
                : "ROLE_" + role.name();

        return new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                user.getPassword(),
                user.isEnabled(), // enabled
                true, // account non expired
                true, // credentials non expired
                true, // account non locked
                List.of(new SimpleGrantedAuthority(authority))
        );
    }
}
