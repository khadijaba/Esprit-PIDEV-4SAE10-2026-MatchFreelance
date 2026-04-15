package tn.esprit.evaluation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ObjectifThemeRequest {
    @NotNull
    private Long examenId;
    @NotBlank
    private String theme;
    /** Cible 0–100, ex. 80 */
    private Integer objectifScore;
}
