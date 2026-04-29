package tn.esprit.evaluation.dto;

import lombok.*;

/**
 * Score pondéré agrégé par compétence (skill) sur un passage d'examen.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SkillScoreDto {

    /** Libellé compétence (champ {@code skill} de la question, ou à défaut {@code theme}, ou « Autres »). */
    private String skill;
    private int pointsObtenus;
    private int pointsMax;
    /** 0–100, même règle de plancher que le score global. */
    private int pourcentage;
    private int bonnesReponses;
    private int totalQuestions;
}
