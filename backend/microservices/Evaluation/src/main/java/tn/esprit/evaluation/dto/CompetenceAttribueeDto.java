package tn.esprit.evaluation.dto;

import lombok.*;

/** Compétence attribuée ou mise à jour après obtention du certificat (microservice Skill). */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CompetenceAttribueeDto {

    private Long skillId;
    private String nom;
    /** Ex. WEB_DEVELOPMENT (aligné SkillCategory côté Skill). */
    private String categorie;
    private String niveau;
    /** CREE, DEJA_PRESENTE, INDISPONIBLE */
    private String statut;
}
