package esprit.project.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Note client (1–5) après mission, utilisée pour enrichir le score de réussite affiché.
 */
@Entity
@Table(
        name = "client_freelancer_rating",
        uniqueConstraints = @UniqueConstraint(columnNames = {"project_id", "freelancer_id"})
)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClientFreelancerRating {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Column(name = "project_id", nullable = false)
    private Long projectId;

    @NotNull
    @Column(name = "freelancer_id", nullable = false)
    private Long freelancerId;

    @NotNull
    @Min(1)
    @Max(5)
    @Column(nullable = false)
    private Integer rating;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
