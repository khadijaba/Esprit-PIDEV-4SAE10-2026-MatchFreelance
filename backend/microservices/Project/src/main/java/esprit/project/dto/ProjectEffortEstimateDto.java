package esprit.project.dto;

import lombok.Builder;
import lombok.Value;

import java.util.List;

/**
 * Estimation indicative de charge en jours-homme à partir du texte et des métadonnées du projet.
 */
@Value
@Builder
public class ProjectEffortEstimateDto {
    long projectId;
    /** Point d’estimation jours-homme (effort, pas calendaire). */
    int estimatedManDays;
    int estimatedManDaysLow;
    int estimatedManDaysHigh;
    /** Durée calendaire déclarée (jours). */
    int declaredDurationDays;
    /** Capacité « une personne à temps plein » sur la durée déclarée (jours calendaires). */
    int impliedCapacityOneFteDays;
    /**
     * &gt; 1 si l’effort estimé dépasse ce qu’une seule personne peut faire dans la durée annoncée
     * (ordre de grandeur).
     */
    double fteRequiredVsDeclared;
    List<String> flags;
    String summary;
    String methodVersion;
}
