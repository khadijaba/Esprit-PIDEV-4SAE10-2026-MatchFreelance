package tn.esprit.evaluation.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import tn.esprit.evaluation.domain.TypeParcours;

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

    /** Mode de passage : ENTRAINEMENT (pas d'enregistrement certifiant) ou CERTIFIANT (certificat possible ; correction renvoyee aussi apres soumission). */
    @Builder.Default
    private ModePassage mode = ModePassage.CERTIFIANT;

    /**
     * Doit correspondre au même parcours que celui utilisé pour charger les questions ({@code GET .../passage}).
     */
    @Builder.Default
    private TypeParcours typeParcours = TypeParcours.STANDARD;

    /**
     * Si vrai : passage en révision ciblée (sous-ensemble de questions déjà ratées au moins une fois).
     * Uniquement avec {@link ModePassage#ENTRAINEMENT} ; le nombre minimum de questions du parcours ne s'applique pas.
     */
    private Boolean revisionCiblee;

    public enum ModePassage {
        ENTRAINEMENT,
        CERTIFIANT
    }
}
