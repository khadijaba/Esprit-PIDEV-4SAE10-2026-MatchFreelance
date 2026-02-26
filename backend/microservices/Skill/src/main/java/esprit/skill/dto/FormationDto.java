package esprit.skill.dto;

import lombok.Data;

import java.time.LocalDate;

/** DTO pour les formations récupérées depuis le microservice Formation. */
@Data
public class FormationDto {
    private Long id;
    private String titre;
    private String typeFormation;
    private String description;
    private Integer dureeHeures;
    private LocalDate dateDebut;
    private LocalDate dateFin;
    private Integer capaciteMax;
    private String statut;
}
