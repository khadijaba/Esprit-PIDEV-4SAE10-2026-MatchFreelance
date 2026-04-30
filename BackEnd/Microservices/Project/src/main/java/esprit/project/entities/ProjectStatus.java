package esprit.project.entities;

public enum ProjectStatus {
    /** Brouillon : visible uniquement par le project owner jusqu’à publication (OPEN). */
    DRAFT,
    OPEN,
    IN_PROGRESS,
    COMPLETED,
    CANCELLED
}
