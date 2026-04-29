package Config;

import Entity.User;
import Entity.Role;
import Repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.dao.DataAccessException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDate;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final String DEFAULT_ADMIN_EMAIL = "admin@app.com";
    /** Mot de passe par défaut (respecter majuscules / symboles). */
    private static final String DEFAULT_ADMIN_PASSWORD = "Admin@123";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JdbcTemplate jdbcTemplate;

    /**
     * Si true : à chaque démarrage, admin@app.com retrouve le mot de passe par défaut et {@code enabled=true}.
     * Utile en dev si la base a un hash incorrect ; mettre à false en production.
     */
    @Value("${app.admin.repair-default-account:true}")
    private boolean repairDefaultAccount;

    public DataInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder, JdbcTemplate jdbcTemplate) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(String... args) {
        if (!usersTableExists()) {
            System.out.println("Initialisation admin ignoree: table users introuvable.");
            return;
        }

        alignRoleColumnIfNeeded();

        User defaultAdmin = userRepository.findByEmailIgnoreCase(DEFAULT_ADMIN_EMAIL).orElse(null);

        if (defaultAdmin == null) {
            User admin = User.builder()
                    .firstName("Admin")
                    .lastName("System")
                    .address("Tunis")
                    .email(DEFAULT_ADMIN_EMAIL)
                    .password(passwordEncoder.encode(DEFAULT_ADMIN_PASSWORD))
                    .birthDate(LocalDate.of(1990, 1, 1))
                    .role(Role.ADMIN)
                    .enabled(true)
                    .build();
            userRepository.save(admin);
            System.out.println("Admin cree : " + DEFAULT_ADMIN_EMAIL + " / " + DEFAULT_ADMIN_PASSWORD);
        }

        if (repairDefaultAccount) {
            User toRepair = userRepository.findByEmailIgnoreCase(DEFAULT_ADMIN_EMAIL).orElse(null);
            if (toRepair != null) {
                toRepair.setRole(Role.ADMIN);
                toRepair.setPassword(passwordEncoder.encode(DEFAULT_ADMIN_PASSWORD));
                toRepair.setEnabled(true);
                userRepository.save(toRepair);
                System.out.println("Compte admin par defaut resynchronise : " + DEFAULT_ADMIN_EMAIL + " / " + DEFAULT_ADMIN_PASSWORD);
            }
        }
    }

    private boolean usersTableExists() {
        try {
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'users'",
                    Integer.class);
            return count != null && count > 0;
        } catch (DataAccessException e) {
            System.out.println("Verification de la table users impossible: " + e.getMessage());
            return false;
        }
    }

    /**
     * Correctif de compatibilité : certaines bases historiques gardent un ENUM
     * incomplet sur users.role, causant "Data truncated for column 'role'".
     * On force un type VARCHAR et on aligne CLIENT -> PROJECT_OWNER.
     */
    private void alignRoleColumnIfNeeded() {
        try {
            jdbcTemplate.execute("ALTER TABLE users MODIFY COLUMN role VARCHAR(32) NOT NULL");
            jdbcTemplate.update("UPDATE users SET role = 'PROJECT_OWNER' WHERE role = 'CLIENT'");
        } catch (Exception e) {
            System.out.println("Alignement role ignore (table/colonne indisponible ou deja conforme): " + e.getMessage());
        }
    }
}
