package tn.esprit.evaluation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ObjectifThemeDto {
    private Long id;
    private Long examenId;
    private String theme;
    private Integer objectifScore;
    private boolean actif;
    /** Dernier score % sur ce thème (entraînement), ou null si aucune donnée. */
    private Integer dernierScoreTheme;
    private boolean objectifAtteint;
}
