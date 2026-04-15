package tn.esprit.evaluation.dto;

import lombok.*;

/** Projet du marché (microservice Project) proposé selon domaine + niveau. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjetMarcheDto {

    private Long id;
    private String titre;
    private Double budget;
    private Integer dureeJours;
    private String statut;
    private String raison;
    /**
     * 0–100 : adéquation entre les compétences requises du projet et le profil Skill du freelancer
     * (plus élevé = meilleure correspondance).
     */
    private Integer scoreAlignementSkills;
}
