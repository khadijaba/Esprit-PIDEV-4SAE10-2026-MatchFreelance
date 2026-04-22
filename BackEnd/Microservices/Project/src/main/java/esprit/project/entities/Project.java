package esprit.project.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
<<<<<<< HEAD
@Table(name = "project")
=======
<<<<<<< HEAD
=======
@Table(name = "project")
>>>>>>> 8d5250d (Ajout du projet MatchFreelance)
>>>>>>> b7e93fa9abcd913d3ba37913b8481d5dd480ed43
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Project {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Title is required")
    @Column(nullable = false)
    private String title;

    @NotBlank(message = "Description is required")
    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @NotNull(message = "Budget is required")
    @Positive(message = "Budget must be positive")
    @Column(nullable = false)
    private Double budget;

    @NotNull(message = "Duration is required")
    @Positive(message = "Duration must be positive")
    @Column(nullable = false)
    private Integer duration; // in days

    @Enumerated(EnumType.STRING)
<<<<<<< HEAD
    @Column(nullable = false, columnDefinition = "VARCHAR(32)")
=======
<<<<<<< HEAD
    @Column(nullable = false)
=======
    @Column(nullable = false, columnDefinition = "VARCHAR(32)")
>>>>>>> 8d5250d (Ajout du projet MatchFreelance)
>>>>>>> b7e93fa9abcd913d3ba37913b8481d5dd480ed43
    private ProjectStatus status;

    @Column(name = "project_owner_id", nullable = false)
    private Long projectOwnerId;

    @ElementCollection
    @CollectionTable(name = "project_required_skills", joinColumns = @JoinColumn(name = "project_id"))
    @Column(name = "skill_name")
    private List<String> requiredSkills = new ArrayList<>();

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) {
            status = ProjectStatus.OPEN;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
