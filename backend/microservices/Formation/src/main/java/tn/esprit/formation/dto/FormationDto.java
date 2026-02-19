package tn.esprit.formation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;
import tn.esprit.formation.entity.Formation;
import tn.esprit.formation.entity.TypeFormation;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FormationDto {

    private Long id;
    @NotBlank
    private String titre;
    private TypeFormation typeFormation;
    private String description;
    @NotNull
    @Positive
    private Integer dureeHeures;
    @NotNull
    private LocalDate dateDebut;
    @NotNull
    private LocalDate dateFin;
    @Positive
    private Integer capaciteMax;
    private Formation.StatutFormation statut;

    public static FormationDto fromEntity(Formation f) {
        return FormationDto.builder()
                .id(f.getId())
                .titre(f.getTitre())
                .typeFormation(f.getTypeFormation() != null ? f.getTypeFormation() : TypeFormation.WEB_DEVELOPMENT)
                .description(f.getDescription())
                .dureeHeures(f.getDureeHeures())
                .dateDebut(f.getDateDebut())
                .dateFin(f.getDateFin())
                .capaciteMax(f.getCapaciteMax())
                .statut(f.getStatut())
                .build();
    }

    public Formation toEntity() {
        return Formation.builder()
                .id(id)
                .titre(titre)
                .typeFormation(typeFormation != null ? typeFormation : TypeFormation.WEB_DEVELOPMENT)
                .description(description)
                .dureeHeures(dureeHeures)
                .dateDebut(dateDebut)
                .dateFin(dateFin)
                .capaciteMax(capaciteMax)
                .statut(statut != null ? statut : Formation.StatutFormation.OUVERTE)
                .build();
    }
}
