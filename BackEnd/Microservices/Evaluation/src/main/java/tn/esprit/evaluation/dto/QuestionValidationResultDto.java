package tn.esprit.evaluation.dto;

import lombok.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Retour du validateur IA (Ollama / OpenAI) pour une question QCM.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuestionValidationResultDto {

    private boolean llmConfigured;
    /** JSON modèle correctement parsé */
    private boolean parseOk;
    /** 1–5 : clarté de l'énoncé et des options */
    private Integer clairScore;
    /** 1–5 : cohérence technique / factualité au regard des choix */
    private Integer correctTechniqueScore;
    private Boolean ambigu;
    private String ambiguDetails;
    /** Recommandation : peut être publiée telle quelle ou après micro-retouches */
    private Boolean publishable;
    private String summary;
    @Builder.Default
    private List<String> suggestions = new ArrayList<>();
    /** Erreur réseau, parsing, ou message brut si échec */
    private String errorMessage;
}
