package tn.esprit.evaluation.dto;

import lombok.*;

/**
 * Corps pour {@code POST .../validate-question-ai} (ou {@code .../questions/validate-ai}) — QCM à auditer avant publication.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuestionValidationRequest {

    private String enonce;
    private String optionA;
    private String optionB;
    private String optionC;
    private String optionD;
    /** A, B, C ou D */
    private String bonneReponse;
    private String theme;
    private String skill;
    /** Contexte optionnel (titre formation, objectifs, extrait de cours…) */
    private String contexteFormation;
}
