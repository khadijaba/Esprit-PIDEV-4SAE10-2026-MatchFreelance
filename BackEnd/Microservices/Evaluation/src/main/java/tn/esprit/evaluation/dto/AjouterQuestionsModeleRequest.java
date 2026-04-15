package tn.esprit.evaluation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Ajoute des questions « squelette » à un examen existant (administration).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AjouterQuestionsModeleRequest {

    /** Nombre de questions à créer (1–50). */
    private Integer nombre;

    /** COMMUN (défaut), STANDARD ou RENFORCEMENT. */
    private String parcoursInclusion;

    /** FACILE, MOYEN ou DIFFICILE (défaut MOYEN). */
    private String niveauDifficulte;
}
