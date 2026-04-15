package esprit.project.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "phase_deliverable")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PhaseDeliverable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "phase_id", nullable = false)
    private ProjectPhase phase;

    @NotBlank
    @Column(nullable = false)
    private String title;

    @Column(length = 2000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DeliverableType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DeliverableReviewStatus reviewStatus = DeliverableReviewStatus.PENDING;

    @Column(length = 2000)
    private String reviewComment;

    private LocalDateTime submittedAt;
    private LocalDateTime reviewedAt;
}
