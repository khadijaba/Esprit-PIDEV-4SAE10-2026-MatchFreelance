package tn.esprit.formation.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Entité Formation - Session de formation pour les freelancers (Project Matching Platform).
 */
@Entity
@Table(name = "formation")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Formation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Le titre est obligatoire")
    @Column(nullable = false)
    private String titre;

    @Enumerated(EnumType.STRING)
    @Column(name = "type_formation")
    @Builder.Default
    private TypeFormation typeFormation = TypeFormation.WEB_DEVELOPMENT;

    @Column(length = 2000)
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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private StatutFormation statut = StatutFormation.OUVERTE;

<<<<<<< HEAD
=======
<<<<<<< HEAD
=======
>>>>>>> b7e93fa9abcd913d3ba37913b8481d5dd480ed43
    /** Niveau de la formation (Débutant, Intermédiaire, Avancé). */
    @Enumerated(EnumType.STRING)
    @Column(name = "niveau", length = 20)
    private NiveauFormation niveau;

    /** Si non null : accès réservé aux freelancers ayant le certificat de cet examen (examenId côté Evaluation). */
    @Column(name = "examen_requis_id")
    private Long examenRequisId;

<<<<<<< HEAD
=======
>>>>>>> 8d5250d (Ajout du projet MatchFreelance)
>>>>>>> b7e93fa9abcd913d3ba37913b8481d5dd480ed43
    @OneToMany(mappedBy = "formation", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Inscription> inscriptions = new ArrayList<>();

<<<<<<< HEAD
=======
<<<<<<< HEAD
=======
>>>>>>> b7e93fa9abcd913d3ba37913b8481d5dd480ed43
    @OneToMany(mappedBy = "formation", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("ordre ASC")
    @Builder.Default
    private List<Module> modules = new ArrayList<>();

<<<<<<< HEAD
=======
>>>>>>> 8d5250d (Ajout du projet MatchFreelance)
>>>>>>> b7e93fa9abcd913d3ba37913b8481d5dd480ed43
    public enum StatutFormation {
        OUVERTE,
        EN_COURS,
        TERMINEE,
        ANNULEE
    }
}
