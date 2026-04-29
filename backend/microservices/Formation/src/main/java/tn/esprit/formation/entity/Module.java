package tn.esprit.formation.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.*;

/**
 * Module court : séquence pédagogique d'une formation (contenu découpé en petits modules).
 * Une formation peut avoir plusieurs modules, ordonnés par ordre d'affichage.
 */
@Entity
@Table(name = "module_formation")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Module {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Le titre du module est obligatoire")
    @Column(nullable = false, length = 255)
    private String titre;

    @Column(length = 2000)
    private String description;

    /** Durée du module en minutes (module court). */
    @NotNull
    @Positive
    @Column(name = "duree_minutes", nullable = false)
    private Integer dureeMinutes;

    /** Ordre d'affichage dans la formation (1, 2, 3...). */
    @NotNull
    @PositiveOrZero
    @Column(nullable = false)
    @Builder.Default
    private Integer ordre = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "formation_id", nullable = false)
    private Formation formation;
}
