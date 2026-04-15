package tn.esprit.evaluation.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Objectif personnel : viser un % sur un thème pour un examen donné.
 */
@Entity
@Table(
        name = "freelancer_objectif_theme",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_objectif_freelancer_examen_theme",
                columnNames = {"freelancer_id", "examen_id", "theme"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FreelancerObjectifTheme {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "freelancer_id", nullable = false)
    private Long freelancerId;

    @Column(name = "examen_id", nullable = false)
    private Long examenId;

    @Column(nullable = false, length = 120)
    private String theme;

    /** Cible 0–100 (ex. 80). */
    @Column(name = "objectif_score", nullable = false)
    private Integer objectifScore;

    @Column(nullable = false)
    @Builder.Default
    private boolean actif = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
