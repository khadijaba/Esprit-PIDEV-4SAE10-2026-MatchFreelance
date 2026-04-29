package tn.esprit.evaluation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * Recommandations de projets pour un freelancer, triées par score d'alignement compétences.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FreelancerProjectMatchingDto {

    private Long freelancerId;
    private Integer profileSkillTokens;
    /** Score global examens (0–100), null si aucune tentative enregistrée. */
    private Integer freelancerExamGlobalScore;
    private List<ProjetMarcheDto> projects;
}
