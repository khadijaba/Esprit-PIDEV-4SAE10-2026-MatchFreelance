package com.freelancing.user.config;

import com.freelancing.user.entity.User;
import com.freelancing.user.enums.UserRole;
import com.freelancing.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Seeds demo users for evaluation. Passwords: client123, freelancer123, admin123.
 * Runs only when the user table is empty; otherwise ensures seed users exist and resets their passwords.
 */
@Component
@RequiredArgsConstructor
public class DataLoader implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    private static final String CLIENT_EMAIL = "client@freelancehub.demo";
    private static final String FREELANCER1_EMAIL = "freelancer1@freelancehub.demo";
    private static final String FREELANCER2_EMAIL = "freelancer2@freelancehub.demo";
    private static final String FREELANCER3_EMAIL = "freelancer3@freelancehub.demo";
    private static final String FREELANCER4_EMAIL = "freelancer4@freelancehub.demo";
    private static final String ADMIN_EMAIL = "admin@freelancehub.demo";
    private static final String PWD_CLIENT = "client123";
    private static final String PWD_FREELANCER = "freelancer123";
    private static final String PWD_ADMIN = "admin123";

    @Override
    public void run(String... args) {
        if (userRepository.count() > 0) {
            ensureSeedUsersExist();
            return;
        }

        User client = createUser(CLIENT_EMAIL, "Marie Martin", PWD_CLIENT, UserRole.CLIENT);
        userRepository.save(client);

        User fl1 = createUser(FREELANCER1_EMAIL, "Jean Dupont", PWD_FREELANCER, UserRole.FREELANCER);
        userRepository.save(fl1);

        User admin = createUser(ADMIN_EMAIL, "Admin FreelanceHub", PWD_ADMIN, UserRole.ADMIN);
        userRepository.save(admin);

        User fl2 = createUser(FREELANCER2_EMAIL, "Sophie Bernard", PWD_FREELANCER, UserRole.FREELANCER);
        userRepository.save(fl2);

        User fl3 = createUser(FREELANCER3_EMAIL, "Lucas Petit", PWD_FREELANCER, UserRole.FREELANCER);
        userRepository.save(fl3);

        User fl4 = createUser(FREELANCER4_EMAIL, "Emma Laurent", PWD_FREELANCER, UserRole.FREELANCER);
        userRepository.save(fl4);
    }

    private User createUser(String email, String name, String password, UserRole role) {
        User u = new User();
        u.setEmail(email);
        u.setName(name);
        u.setPasswordHash(passwordEncoder.encode(password));
        u.setRole(role);
        return u;
    }

    private void ensureSeedUsersExist() {
        saveIfMissing(CLIENT_EMAIL, "Marie Martin", PWD_CLIENT, UserRole.CLIENT);
        saveIfMissing(FREELANCER1_EMAIL, "Jean Dupont", PWD_FREELANCER, UserRole.FREELANCER);
        saveIfMissing(FREELANCER2_EMAIL, "Sophie Bernard", PWD_FREELANCER, UserRole.FREELANCER);
        saveIfMissing(FREELANCER3_EMAIL, "Lucas Petit", PWD_FREELANCER, UserRole.FREELANCER);
        saveIfMissing(FREELANCER4_EMAIL, "Emma Laurent", PWD_FREELANCER, UserRole.FREELANCER);
        saveIfMissing(ADMIN_EMAIL, "Admin FreelanceHub", PWD_ADMIN, UserRole.ADMIN);
    }

    private void saveIfMissing(String email, String name, String password, UserRole role) {
        if (userRepository.findByEmail(email).isEmpty()) {
            userRepository.save(createUser(email, name, password, role));
        }
    }
}
