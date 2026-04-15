package tn.esprit.evaluation.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Temps majoré (accessibilité) : multiplicateur appliqué au chrono par question.
 */
@Entity
@Table(
        name = "freelancer_amenagement_temps",
        uniqueConstraints = @UniqueConstraint(name = "uk_amenagement_freelancer", columnNames = "freelancer_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FreelancerAmenagementTemps {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "freelancer_id", nullable = false)
    private Long freelancerId;

    /** Ex. 1.5 = +50 % de temps par question. */
    @Column(name = "multiplicateur_chrono", nullable = false)
    @Builder.Default
    private double multiplicateurChrono = 1.0;

    @Column(length = 255)
    private String motif;

    @Column(nullable = false)
    @Builder.Default
    private boolean actif = true;

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();
}
