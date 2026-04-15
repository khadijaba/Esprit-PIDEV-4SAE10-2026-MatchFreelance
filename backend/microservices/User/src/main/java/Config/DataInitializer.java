package Config;

import Entity.User;
import Entity.Role;
import Repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
@ConditionalOnProperty(prefix = "matchfreelance", name = "seed-admin", havingValue = "true", matchIfMissing = true)
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final boolean seedAdminForceReset;

    public DataInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder,
            @Value("${matchfreelance.seed-admin-force-reset:false}") boolean seedAdminForceReset) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.seedAdminForceReset = seedAdminForceReset;
    }

    @Override
    public void run(String... args) {
        if (seedAdminForceReset) {
            userRepository.findByEmail("admin@app.com").ifPresent(admin -> {
                admin.setPassword(passwordEncoder.encode("Admin@123"));
                admin.setEnabled(true);
                admin.setRole(Role.ADMIN);
                userRepository.save(admin);
                System.out.println(
                        "matchfreelance.seed-admin-force-reset : mot de passe réinitialisé pour admin@app.com → Admin@123");
            });
        }
        if (!userRepository.existsByEmail("admin@app.com")) {
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