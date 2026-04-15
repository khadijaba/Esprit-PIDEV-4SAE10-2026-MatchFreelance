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
    private List<ProjetMarcheDto> projects;
}
