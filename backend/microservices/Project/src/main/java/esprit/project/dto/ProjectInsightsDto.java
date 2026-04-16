package esprit.project.dto;

import lombok.Builder;
import lombok.Value;

import java.util.List;

/**
 * Indicateurs avancés sur un projet : qualité de l’annonce, attractivité marché,
 * prédiction de recrutement (heuristiques v1 — pas de ML externe).
 */
@Value
@Builder
public class ProjectInsightsDto {
    long projectId;
    /** 0–100 : titre, description, compétences, cohérence budget/durée. */
    int listingQualityScore;
    /** 0–100 : taux journalier vs fourchette « marché » (TND, tunable). */
    int marketAttractivenessScore;
    /** 0–100 : santé globale (moyenne pondérée qualité / attractivité). */
    int compositeHealthScore;
    /** 0–100 : probabilité de bon déroulement (qualité + historique porteur + dynamique candidatures). */
    int successLikelihoodScore;
    /** LOW / MEDIUM / HIGH — complexité du profil recherché (nombre de compétences). */
    String nicheDifficulty;
    /** Borne basse jours avant shortlist réaliste. */
    int estimatedDaysToHireLow;
    /** Borne haute. */
    int estimatedDaysToHireHigh;
    /** Budget / durée (TND par jour). */
    double dailyRate;
    /** Nombre de candidatures (null si service Candidature indisponible). */
    Integer applicationCount;
    long ownerCompletedProjects;
    long ownerOpenProjects;
    long ownerTotalProjects;
    /** LOW / MEDIUM / HIGH — alertes cumulées. */
    String riskLevel;
    List<String> flags;
    String summary;
    String modelVersion;
    String computedAt;
}
