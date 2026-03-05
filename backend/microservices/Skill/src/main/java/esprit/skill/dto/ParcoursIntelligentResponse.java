package esprit.skill.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/** Réponse du parcours intelligent : compétences actuelles, gaps, formations proposées. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParcoursIntelligentResponse {

    private Long freelancerId;

    /** Compétences actuelles du freelancer (DTO pour sérialisation JSON fiable). */
    private List<SkillDto> competencesActuelles;

    /** Catégories / domaines où le freelancer a des compétences. */
    private List<String> categoriesActuelles;

    /** Gaps détectés : domaines sans compétence ou à renforcer (alignés sur TypeFormation). */
    private List<String> gapsDetectes;

    /** Formations ciblées proposées (du microservice Formation) pour combler les gaps. */
    private List<FormationDto> formationsProposees;
}
