package esprit.project.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO pour les réponses du microservice User (sans mot de passe).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserDto {

    private Long id;
    private String email;
    private String role;
    private String firstName;
    private String lastName;
    private LocalDateTime createdAt;

    /** Nom affichable (l'API User expose firstName/lastName, pas fullName). */
    public String getFullName() {
        String a = firstName != null ? firstName.trim() : "";
        String b = lastName != null ? lastName.trim() : "";
        String joined = (a + " " + b).trim();
        return joined.isEmpty() ? null : joined;
    }
}
