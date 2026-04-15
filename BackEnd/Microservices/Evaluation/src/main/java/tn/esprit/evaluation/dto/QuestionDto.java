package tn.esprit.evaluation.dto;

import lombok.*;
import tn.esprit.evaluation.entity.Question;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuestionDto {

    private Long id;
    private Long examenId;
    private Integer ordre;
    private String enonce;  // optionnel en création, défaut "Question N" côté service
    private String optionA;
    private String optionB;
    private String optionC;
    private String optionD;
    private String bonneReponse; // A, B, C ou D (défaut A côté service)

    /** COMMUN, STANDARD ou RENFORCEMENT (parcours différenciés). */
    private String parcoursInclusion;

    /** Thème pour recommandations de révision (modules Formation). */
    private String theme;

    /** Compétence évaluée (score par compétence) ; si absent, le thème est utilisé comme repli. */
    private String skill;

    /** FACILE, MOYEN, DIFFICILE — examen adaptatif. */
    private String niveauDifficulte;

    /** Explication affichée après correction (optionnel). */
    private String explication;

    public static QuestionDto fromEntity(Question q) {
        return fromEntity(q, q.getExamen() != null ? q.getExamen().getId() : null);
    }

    /** Utiliser cet overload pour éviter d’accéder à la relation LAZY examen (évite LazyInitializationException). */
    public static QuestionDto fromEntity(Question q, Long examenId) {
        String pi = q.getParcoursInclusion() != null ? q.getParcoursInclusion().name() : "COMMUN";
        String nd = q.getNiveauDifficulte() != null ? q.getNiveauDifficulte().name() : "MOYEN";
        return QuestionDto.builder()
                .id(q.getId())
                .examenId(examenId)
                .ordre(q.getOrdre())
                .enonce(q.getEnonce())
                .optionA(q.getOptionA())
                .optionB(q.getOptionB())
                .optionC(q.getOptionC())
                .optionD(q.getOptionD())
                .bonneReponse(q.getBonneReponse())
                .parcoursInclusion(pi)
                .theme(q.getTheme())
                .skill(q.getSkill())
                .niveauDifficulte(nd)
                .explication(q.getExplication())
                .build();
    }
}
