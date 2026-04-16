package esprit.project.dto;

import lombok.Data;

/** Réponse JSON de {@code GET /api/candidatures/project/{id}/application-count}. */
@Data
public class ApplicationCountResponse {
    private long count;
}
