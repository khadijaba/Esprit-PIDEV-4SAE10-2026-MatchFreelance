package com.freelancing.user.config;

import com.freelancing.user.entity.User;
import com.freelancing.user.enums.UserRole;
import com.freelancing.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * Seeds freelancing_users with one admin, one client, and 7 freelancers.
 * Real-life Tunisian names, new emails/passwords. Old demo accounts are not created.
 */
@Component
@RequiredArgsConstructor
public class UserDataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;

    @Override
    public void run(String... args) {
        if (userRepository.count() > 0) {
            return;
        }

        // 1 Admin
        saveUser("Khalil Mejri", "admin@matchfreelance.tn", "Admin2025!", UserRole.ADMIN, null, "Tunis", null, null);

        // 3 Clients (project owners)
        saveUser("Sarra Ben Ammar", "sarra.benammar@matchfreelance.tn", "Client2025!", UserRole.CLIENT,
                "12 Avenue Habib Bourguiba", "Tunis", 36.8065, 10.1815);
        saveUser("Chaker Mourad", "chaker.mourad@matchfreelance.tn", "Client2025!", UserRole.CLIENT,
                "20 Rue de Marseille", "Tunis", 36.8065, 10.1815);
        saveUser("Salma Ferchichi", "salma.ferchichi@matchfreelance.tn", "Client2025!", UserRole.CLIENT,
                "5 Avenue Taïeb Mhiri", "Sfax", 34.7406, 10.7603);

        // 7 Freelancers (Tunisian names, Tunisian cities)
        saveUser("Amira Ben Ali", "amira.benali@matchfreelance.tn", "Freelancer2025!", UserRole.FREELANCER,
                "45 Rue de la République", "Sfax", 34.7406, 10.7603);
        saveUser("Youssef Trabelsi", "youssef.trabelsi@matchfreelance.tn", "Freelancer2025!", UserRole.FREELANCER,
                "8 Rue Hedi Chaker", "Sousse", 35.8256, 10.6346);
        saveUser("Nadia Jlassi", "nadia.jlassi@matchfreelance.tn", "Freelancer2025!", UserRole.FREELANCER,
                "22 Avenue Farhat Hached", "Sousse", 35.8256, 10.6346);
        saveUser("Omar Khelifi", "omar.khelifi@matchfreelance.tn", "Freelancer2025!", UserRole.FREELANCER,
                "5 Rue Ali Belhouane", "Tunis", 36.8065, 10.1815);
        saveUser("Ines Hammami", "ines.hammami@matchfreelance.tn", "Freelancer2025!", UserRole.FREELANCER,
                "14 Avenue de la Liberté", "Nabeul", 36.4561, 10.7376);
        saveUser("Mehdi Gharbi", "mehdi.gharbi@matchfreelance.tn", "Freelancer2025!", UserRole.FREELANCER,
                "3 Rue Ibn Khaldoun", "Bizerte", 37.2744, 9.8739);
        saveUser("Leila Mansour", "leila.mansour@matchfreelance.tn", "Freelancer2025!", UserRole.FREELANCER,
                "19 Boulevard du 7 Novembre", "Monastir", 35.7770, 10.8261);
    }

    private void saveUser(String fullName, String email, String password, UserRole role,
                          String addressLine, String city, Double lat, Double lng) {
        User u = new User();
        u.setFullName(fullName);
        u.setEmail(email);
        u.setPassword(password);
        u.setRole(role);
        u.setAddressLine(addressLine);
        u.setCity(city);
        u.setLat(lat);
        u.setLng(lng);
        userRepository.save(u);
    }
}
