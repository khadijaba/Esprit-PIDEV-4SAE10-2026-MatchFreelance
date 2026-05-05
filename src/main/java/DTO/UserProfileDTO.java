package DTO;

import Entity.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileDTO {
    private Long id;
    private String firstName;
    private String lastName;
    private String address;
    private String email;
    private LocalDate birthDate;
    private Role role;
    private String profilePicture;
    
    // Note: Le mot de passe n'est jamais inclus dans le DTO pour des raisons de sécurité
}
