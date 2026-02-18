package tn.esprit.evaluation.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.List;

/**
 * Réponses soumises par le freelancer pour un examen.
 * questionId -> choix (A, B, C ou D)
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReponseExamenRequest {

    @NotNull
    private Long freelancerId;

    /** Liste des réponses: index = ordre question, value = A/B/C/D */
    @NotNull
    private List<String> reponses; // ex: ["A", "C", "B", "D", "A"]
}
