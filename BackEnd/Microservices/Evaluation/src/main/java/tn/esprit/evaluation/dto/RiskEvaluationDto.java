package tn.esprit.evaluation.dto;

import lombok.*;
import tn.esprit.evaluation.domain.TypeParcours;

import java.util.ArrayList;
import java.util.List;

/** Évaluation du risque d'échec (règles métier + signaux Formation / Skill). */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RiskEvaluationDto {

    /** Score 0 = faible risque, 100 = risque élevé. */
    @Builder.Default
    private int scoreRisque = 0;

    /** FAIBLE, MODERE, ELEVE */
    private String niveau;

    private String messageApprenant;

    /** Si true, le formateur doit être notifié / surveiller la tentative. */
    @Builder.Default
    private boolean alerteFormateur = false;

    /** Texte court pour espace formateur / admin. */
    private String messageFormateur;

    @Builder.Default
    private List<String> facteurs = new ArrayList<>();

    /** Parcours suggéré avant le passage. */
    private TypeParcours parcoursRecommande;
}
