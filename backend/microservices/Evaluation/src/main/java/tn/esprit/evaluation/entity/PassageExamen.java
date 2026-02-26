package tn.esprit.evaluation.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Passage d'un examen par un freelancer. Stocke le score et le résultat (réussi / échoué).
 */
@Entity
@Table(name = "passage_examen", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"examen_id", "freelancer_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PassageExamen {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Column(name = "freelancer_id", nullable = false)
    private Long freelancerId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "examen_id", nullable = false)
    private Examen examen;

    /** Score en % (0-100) */
    @Column(nullable = false)
    private Integer score;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ResultatExamen resultat;

    @Column(name = "date_passage", updatable = false)
    @Builder.Default
    private LocalDateTime datePassage = LocalDateTime.now();

    public enum ResultatExamen {
        REUSSI,
        ECHOUE
    }
}
