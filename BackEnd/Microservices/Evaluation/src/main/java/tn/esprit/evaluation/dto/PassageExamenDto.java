package tn.esprit.evaluation.dto;

import lombok.*;
import tn.esprit.evaluation.domain.TypeParcours;
import tn.esprit.evaluation.entity.PassageExamen;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PassageExamenDto {

    private Long id;
    private Long freelancerId;
    private Long examenId;
    private String examenTitre;
    private Integer score;
    /**
     * Pourcentage sans pondération (bonnes / total), pour comparaison avec {@link #score}
     * lorsque les difficultés ne sont pas toutes identiques.
     */
    private Integer scoreSansPonderation;
    /** Somme des poids des questions (FACILE 1 + MOYEN 2 + DIFFICILE 3). */
    private Integer pointsMax;
    /** Points obtenus (somme des poids des réponses correctes). */
    private Integer pointsObtenus;
    private PassageExamen.ResultatExamen resultat;
    private LocalDateTime datePassage;
    private ReponseExamenRequest.ModePassage mode;
    private Integer totalQuestions;
    private Integer bonnesReponses;
    /** Certificat auto-généré lorsque resultat == REUSSI (renvoyé directement dans la réponse). */
    private CertificatDto certificat;
    /** Correction détaillée renvoyée après soumission (certifiant ou entraînement) ; absente si passage rechargé depuis la base sans rejouer les réponses. */
    private List<CorrectionItemDto> correction;
    /** Analyse automatique: axes d'amélioration détectés sur les erreurs. */
    private List<String> analyseErreurs;
    /** Message intelligent personnalisé selon le résultat. */
    private String messageFeedback;
    /** Feedback : formations recommandées (en lien avec Formation). */
    private List<FormationRecoDto> formationsRecommandees;

    /** Parcours utilisé (lots de questions). */
    private TypeParcours typeParcours;

    /** Modules Formation ciblés selon les thèmes des questions ratées (max 3). */
    private List<ModuleRevisionDto> modulesRevisionCibles;

    /** Synthèse risque (même logique que {@code GET .../parcours/risque/...}). */
    private RiskEvaluationDto evaluationRisque;

    /** Niveau métier déduit du score après réussite certifiante (ex. INTERMEDIAIRE, AVANCE). */
    private String niveauCalcule;
    /** Compétences ajoutées ou signalées sur le profil Skill après certificat. */
    private List<CompetenceAttribueeDto> competencesAttribuees;
    /** Projets ouverts du microservice Project alignés sur le domaine / marché. */
    private List<ProjetMarcheDto> projetsMarcheRecommandes;
    /** Résumé court pour l’apprenant (carrière post-certificat). */
    private String messageCarriere;

    /** Score pondéré par compétence (skill), même logique que le score global. */
    private List<SkillScoreDto> scoreParSkill;

    public static PassageExamenDto fromEntity(PassageExamen p) {
        return PassageExamenDto.builder()
                .id(p.getId())
                .freelancerId(p.getFreelancerId())
                .examenId(p.getExamen().getId())
                .examenTitre(p.getExamen().getTitre()) 
                .score(p.getScore())
                .resultat(p.getResultat())
                .datePassage(p.getDatePassage())
                .typeParcours(p.getTypeParcours())
                .build();
    }
}
