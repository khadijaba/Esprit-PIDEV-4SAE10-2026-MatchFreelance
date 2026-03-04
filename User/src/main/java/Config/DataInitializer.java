package Config;

import Entity.User;
import Entity.Role;
import Repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        // Créer l'admin uniquement s'il n'existe pas
        if (!userRepository.existsByRole(Role.ADMIN)) {
            User admin = User.builder()
                    .firstName("Admin")
                    .lastName("System")
                    .address("Tunis")
                    .email("admin@app.com")
                    .password(passwordEncoder.encode("Admin@123"))
                    .birthDate(LocalDate.of(1990, 1, 1))
                    .role(Role.ADMIN)
                    .build();
            userRepository.save(admin);
            System.out.println("✅ Admin créé : admin@app.com / Admin@123");
        }
    }
}