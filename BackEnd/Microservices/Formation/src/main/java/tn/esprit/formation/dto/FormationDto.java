package tn.esprit.formation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;
import tn.esprit.formation.entity.Formation;
<<<<<<< HEAD
import tn.esprit.formation.entity.NiveauFormation;
=======
<<<<<<< HEAD
=======
import tn.esprit.formation.entity.NiveauFormation;
>>>>>>> 8d5250d (Ajout du projet MatchFreelance)
>>>>>>> b7e93fa9abcd913d3ba37913b8481d5dd480ed43
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

<<<<<<< HEAD
=======
<<<<<<< HEAD
=======
>>>>>>> b7e93fa9abcd913d3ba37913b8481d5dd480ed43
    /** Niveau de la formation (Débutant, Intermédiaire, Avancé). */
    private NiveauFormation niveau;

    /** Id de l'examen dont le certificat est requis pour s'inscrire (null = pas de prérequis). */
    private Long examenRequisId;

<<<<<<< HEAD
=======
>>>>>>> 8d5250d (Ajout du projet MatchFreelance)
>>>>>>> b7e93fa9abcd913d3ba37913b8481d5dd480ed43
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
<<<<<<< HEAD
                .niveau(f.getNiveau())
                .examenRequisId(f.getExamenRequisId())
=======
<<<<<<< HEAD
=======
                .niveau(f.getNiveau())
                .examenRequisId(f.getExamenRequisId())
>>>>>>> 8d5250d (Ajout du projet MatchFreelance)
>>>>>>> b7e93fa9abcd913d3ba37913b8481d5dd480ed43
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
<<<<<<< HEAD
                .niveau(niveau)
                .examenRequisId(examenRequisId)
=======
<<<<<<< HEAD
=======
                .niveau(niveau)
                .examenRequisId(examenRequisId)
>>>>>>> 8d5250d (Ajout du projet MatchFreelance)
>>>>>>> b7e93fa9abcd913d3ba37913b8481d5dd480ed43
                .build();
    }
}
