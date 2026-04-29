package tn.esprit.evaluation.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdaptatifDemarrageDto {

    private String token;
    private QuestionDto question;
    private int numeroQuestion;
    private int questionsTotal;
    /** Libellé difficulté cible pour cette question (FACILE, MOYEN, DIFFICILE). */
    private String difficulteCible;
}
