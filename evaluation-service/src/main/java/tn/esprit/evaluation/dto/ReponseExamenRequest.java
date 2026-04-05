package tn.esprit.evaluation.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.List;

/**
 * Réponses soumises par le freelancer pour un examen.
 * Index = ordre de la question, value = A, B, C ou D.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReponseExamenRequest {

    @NotNull(message = "freelancerId est requis")
    private Long freelancerId;

    /** Liste des réponses (ordre = ordre des questions), chaque élément A, B, C ou D. */
    @NotNull(message = "Les réponses sont requises")
    @NotEmpty(message = "Au moins une réponse est requise")
    private List<String> reponses;

    /** Mode de passage : ENTRAINEMENT (avec correction) ou CERTIFIANT (sans correction, certificat possible). */
    @Builder.Default
    private ModePassage mode = ModePassage.CERTIFIANT;

    public enum ModePassage {
        ENTRAINEMENT,
        CERTIFIANT
    }
}
