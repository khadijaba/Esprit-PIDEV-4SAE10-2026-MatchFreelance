package esprit.project.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "project_phase")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProjectPhase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @NotBlank
    @Column(nullable = false)
    private String name;

    @Column(length = 2000)
    private String description;

    @Column(nullable = false)
    private Integer phaseOrder;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProjectPhaseStatus status = ProjectPhaseStatus.PLANNED;

    private LocalDateTime startDate;
    private LocalDateTime dueDate;
    private LocalDateTime approvedAt;
}
