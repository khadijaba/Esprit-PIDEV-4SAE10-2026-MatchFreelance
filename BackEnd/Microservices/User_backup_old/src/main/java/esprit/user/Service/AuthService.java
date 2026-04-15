package esprit.user.Service;

import esprit.user.dto.AuthResponse;
import esprit.user.dto.LoginRequest;
import esprit.user.dto.RegisterRequest;
import esprit.user.entities.User;
import esprit.user.entities.UserRole;
import esprit.user.Repositories.UserRepository;
import esprit.user.security.JwtTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail().trim().toLowerCase())
                .orElseThrow(() -> new IllegalArgumentException("Invalid email or password"));
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("Invalid email or password");
        }
        return buildAuthResponse(user);
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (request.getRole() == UserRole.ADMIN) {
            throw new IllegalArgumentException("Cannot self-register as admin");
        }
        String email = request.getEmail().trim().toLowerCase();
        if (userRepository.existsByEmail(email)) {
            throw new IllegalStateException("Email already registered");
        }
        User user = new User();
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(request.getRole());
        if (request.getFullName() != null && !request.getFullName().isBlank()) {
            user.setFullName(request.getFullName().trim());
        }
        user = userRepository.save(user);
        return buildAuthResponse(user);
    }

    private AuthResponse buildAuthResponse(User user) {
        String token = jwtTokenService.generateToken(user);
        return AuthResponse.builder()
                .token(token)
                .userId(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole().name())
                .build();
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

    public Optional<User> findByIdForPrincipal(Long id) {
        return userRepository.findById(id);
    }
}
