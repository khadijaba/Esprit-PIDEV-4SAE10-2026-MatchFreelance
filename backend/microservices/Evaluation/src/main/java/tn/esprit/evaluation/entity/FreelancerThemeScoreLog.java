package tn.esprit.evaluation.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Score % par thème après une tentative en entraînement (pour suivre un objectif).
 */
@Entity
@Table(name = "freelancer_theme_score_log")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FreelancerThemeScoreLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "freelancer_id", nullable = false)
    private Long freelancerId;

    @Column(name = "examen_id", nullable = false)
    private Long examenId;

    @Column(nullable = false, length = 120)
    private String theme;

    /** Pourcentage de bonnes réponses sur les questions de ce thème pour cette tentative. */
    @Column(name = "score_percent", nullable = false)
    private Integer scorePercent;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
