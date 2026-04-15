package tn.esprit.evaluation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import tn.esprit.evaluation.domain.TypeParcours;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Plan de remédiation personnalisé après échec ou risque élevé.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RemediationPlanDto {

    private Long freelancerId;
    private Long examenId;
    private LocalDateTime generatedAt;
    private TypeParcours parcoursSuggere;
    private Integer objectifScoreCible;
    private Integer estimationTotaleMinutes;
    /** Progression suggérée initiale (0 au démarrage, puis suivie côté front). */
    private Integer progressionPourcent;
    private String resume;

    @Builder.Default
    private List<RemediationStepDto> etapes = new ArrayList<>();
}
