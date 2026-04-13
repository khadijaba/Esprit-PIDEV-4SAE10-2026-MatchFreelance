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
    /** HIGH / MEDIUM / LOW selon volume d'historique exploitable. */
    String confidence;
    /** Missions ACCEPTED prises en compte (hors projet courant). */
    int pastMissionsConsidered;
    /** Phrase courte pour l’UI. */
    String summary;
}
