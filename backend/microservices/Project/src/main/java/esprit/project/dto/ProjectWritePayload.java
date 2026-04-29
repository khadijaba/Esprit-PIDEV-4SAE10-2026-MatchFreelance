package esprit.project.dto;

import esprit.project.entities.Project;
import esprit.project.entities.ProjectStatus;
import lombok.Data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Corps JSON pour création / mise à jour de projet.
 * Le statut est une chaîne pour éviter les erreurs de désérialisation enum côté Jackson.
 */
@Data
public class ProjectWritePayload {
    private String title;
    private String description;
    private Double budget;
    private Integer duration;
    private String status;
    private Long projectOwnerId;
    private List<String> requiredSkills;

    public Project toProjectForCreate() {
        Project p = new Project();
        p.setTitle(title);
        p.setDescription(description);
        p.setBudget(budget);
        p.setDuration(duration);
        p.setProjectOwnerId(projectOwnerId);
        if (status == null || status.isBlank()) {
            p.setStatus(ProjectStatus.OPEN);
        } else {
            p.setStatus(resolveStatus(status));
        }
        p.setRequiredSkills(requiredSkills != null ? new ArrayList<>(requiredSkills) : new ArrayList<>());
        return p;
    }

    public Project toProjectForUpdateMerge(Project existing) {
        Project p = new Project();
        p.setTitle(title != null ? title : existing.getTitle());
        p.setDescription(description != null ? description : existing.getDescription());
        p.setBudget(budget != null ? budget : existing.getBudget());
        p.setDuration(duration != null ? duration : existing.getDuration());
        p.setProjectOwnerId(projectOwnerId != null ? projectOwnerId : existing.getProjectOwnerId());
        if (status != null && !status.isBlank()) {
            p.setStatus(resolveStatus(status));
        } else {
            p.setStatus(existing.getStatus());
        }
        if (requiredSkills != null) {
            p.setRequiredSkills(new ArrayList<>(requiredSkills));
        } else {
            p.setRequiredSkills(
                    existing.getRequiredSkills() != null ? new ArrayList<>(existing.getRequiredSkills()) : new ArrayList<>());
        }
        return p;
    }

    private static ProjectStatus resolveStatus(String raw) {
        try {
            return ProjectStatus.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "Statut projet inconnu : '" + raw + "'. Valeurs acceptées : " + Arrays.toString(ProjectStatus.values()));
        }
    }
}
