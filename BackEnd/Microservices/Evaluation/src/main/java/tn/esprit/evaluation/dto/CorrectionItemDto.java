package tn.esprit.evaluation.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CorrectionItemDto {
    private Long questionId;
    private Integer ordre;
    private String theme;
    /** Compétence associée (repli thème côté serveur si skill absent). */
    private String skill;
    private String enonce;
    private String reponseChoisie;  // A/B/C/D
    private String bonneReponse;    // A/B/C/D
    private boolean correct;
    /** FACILE | MOYEN | DIFFICILE — utilisé pour la pondération du score. */
    private String niveauDifficulte;
    /** Poids dans le calcul du score (1 / 2 / 3). */
    private Integer poids;
    /** Points comptés sur cette question (0 si erreur, sinon égal au poids). */
    private Integer pointsSurQuestion;
    /** Explication pédagogique (peut être null si non renseignée côté administration). */
    private String explication;
}

