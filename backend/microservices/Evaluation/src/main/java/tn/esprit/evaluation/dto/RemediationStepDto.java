package tn.esprit.evaluation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/**
 * Étape d'un learning path de remédiation.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RemediationStepDto {

    private Integer sequence;
    private Long moduleId;
    private String moduleTitre;
    private Integer dureeEstimeeMinutes;
    private LocalDate dateCible;
    private Integer objectifScoreTheme;
    private String raison;
}
