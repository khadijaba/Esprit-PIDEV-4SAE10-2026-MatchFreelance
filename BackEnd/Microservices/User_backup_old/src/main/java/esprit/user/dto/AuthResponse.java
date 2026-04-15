package esprit.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {

    private String token;
    private Long userId;
    private String email;
    private String fullName;
    /** Valeur enum, ex. FREELANCER, CLIENT, ADMIN (chaîne JSON explicite). */
    private String role;
}
