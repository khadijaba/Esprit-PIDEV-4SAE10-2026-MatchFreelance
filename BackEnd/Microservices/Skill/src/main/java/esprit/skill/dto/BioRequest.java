package esprit.skill.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BioRequest {

    @NotBlank(message = "La bio est obligatoire")
    @Size(max = 2000, message = "La bio ne peut pas dépasser 2000 caractères")
    private String bio;
}
