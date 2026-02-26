package tn.esprit.evaluation.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Examen associé à une formation. Le freelancer le passe pour valider la formation.
 */
@Entity
@Table(name = "examen")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Examen {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** ID de la formation (microservice Formation) */
    @NotNull
    @Column(name = "formation_id", nullable = false)
    private Long formationId;

    @NotBlank
    @Column(nullable = false)
    private String titre;

    @Column(length = 1000)
    private String description;

    /** Seuil en % pour réussir (ex: 60) */
    @NotNull
    @PositiveOrZero
    @Column(name = "seuil_reussi", nullable = false)
    @Builder.Default
    private Integer seuilReussi = 60;

    @OneToMany(mappedBy = "examen", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @OrderBy("ordre ASC")
    private List<Question> questions = new ArrayList<>();
}
