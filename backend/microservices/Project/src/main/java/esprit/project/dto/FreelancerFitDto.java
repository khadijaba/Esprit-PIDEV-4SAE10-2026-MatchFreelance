package esprit.project.dto;

import lombok.Builder;
import lombok.Value;

/**
 * Estimation de durée + score de réussite contextualisé pour un freelancer sur un projet donné.
 */
@Value
@Builder
public class FreelancerFitDto {
    Long freelancerId;
    /** Jours estimés (point). */
    int estimatedDurationDays;
    /** Borne basse (percentile approximatif / historique). */
    int durationLowDays;
    /** Borne haute. */
    int durationHighDays;
    /** 0–100 : adéquation + historique + notes clients si présentes. */
    int successScore;
    /** 0–100 : axe « compétences » pour l’UI (formule affichée 0,7 × comp + 0,3 × exp). */
    int competencesPercent;
    /** 0–100 : axe « expérience » (missions passées). */
    int experiencePercent;
    /** 0–100 : score pondéré affiché = arrondi(0,7 × competences + 0,3 × experience). */
    int weightedMatchPercent;
    /** HIGH / MEDIUM / LOW selon volume d'historique exploitable. */
    String confidence;
    /** Missions ACCEPTED prises en compte (hors projet courant). */
    int pastMissionsConsidered;
    /** Phrase courte pour l’UI. */
    String summary;
}
