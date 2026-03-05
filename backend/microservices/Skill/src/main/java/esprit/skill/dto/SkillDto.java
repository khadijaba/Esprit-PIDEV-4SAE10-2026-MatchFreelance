package esprit.skill.dto;

import esprit.skill.entities.SkillCategory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** DTO pour exposer une compétence dans les réponses API (évite la sérialisation d'entités JPA). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SkillDto {

    private Long id;
    private String name;
    private SkillCategory category;
    private Long freelancerId;
    private String level;
    private Integer yearsOfExperience;
    private LocalDateTime createdAt;
}
