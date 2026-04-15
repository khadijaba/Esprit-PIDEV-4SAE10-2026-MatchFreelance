package tn.esprit.evaluation.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Historique léger par question (toutes tentatives) pour révision ciblée et suivi par thème.
 */
@Entity
@Table(
        name = "freelancer_question_trace",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_freelancer_question_trace",
                columnNames = {"freelancer_id", "question_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FreelancerQuestionTrace {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "freelancer_id", nullable = false)
    private Long freelancerId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    @Column(name = "wrong_count", nullable = false)
    @Builder.Default
    private int wrongCount = 0;

    @Column(name = "correct_count", nullable = false)
    @Builder.Default
    private int correctCount = 0;

    @Column(name = "last_was_wrong", nullable = false)
    @Builder.Default
    private boolean lastWasWrong = false;

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();
}
