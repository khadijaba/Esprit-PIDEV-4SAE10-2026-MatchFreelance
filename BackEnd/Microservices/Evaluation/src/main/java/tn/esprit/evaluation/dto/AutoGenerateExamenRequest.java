package tn.esprit.evaluation.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.*;

/**
 * Paramètres optionnels pour la génération automatique d'un examen depuis une formation.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AutoGenerateExamenRequest {

    /**
     * Obligatoire pour {@code POST /api/examens/auto-generate} ou {@code POST /api/examens/generation/auto-generate}.
     * Pour {@code POST .../formation/{formationId}/auto-generate}, l’ID d’URL prime ; une valeur éventuelle ici est ignorée.
     */
    @Positive
    private Long formationId;

    @PositiveOrZero
    @Max(100)
    @Builder.Default
    private Integer seuilReussi = 60;

    /** Si renseigné, ajouté au titre généré (ex. " — Session avril"). */
    private String suffixeTitre;

    /**
     * {@code true} force l’appel LLM si configuré ; {@code false} le désactive ; {@code null} suit
     * {@code app.examen.llm.use-by-default}.
     */
    private Boolean useLlm;

    /**
     * Si {@code true}, l’examen généré est renvoyé sans être enregistré en base (prévisualisation).
     */
    private Boolean preview;
}
