package tn.esprit.evaluation.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdaptatifEtapeReponseDto {

    private boolean reponseCorrecte;
    private boolean termine;
    /** Difficulté ajustée pour la suite (ou après la dernière réponse si terminé). */
    private String difficulteApresAjustement;
    private Integer numeroQuestion;
    private Integer questionsTotal;
    private QuestionDto prochaineQuestion;
    /** Présent uniquement lorsque {@code termine} est vrai. */
    private PassageExamenDto resultat;
}
