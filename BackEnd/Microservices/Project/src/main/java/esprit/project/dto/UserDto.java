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
    private String fullName;
    private LocalDateTime createdAt;
}
