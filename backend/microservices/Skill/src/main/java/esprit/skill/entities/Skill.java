package esprit.skill.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Skill {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Le nom du skill est obligatoire")
    @Size(min = 2, max = 100, message = "Le nom du skill doit contenir entre 2 et 100 caractères")
    @Column(nullable = false)
    private String name;

    @NotNull(message = "La catégorie est obligatoire")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SkillCategory category;

    @NotNull(message = "L'identifiant du freelancer est obligatoire")
    @Positive(message = "L'identifiant du freelancer doit être positif")
    @Column(nullable = false)
    private Long freelancerId;

    @Pattern(regexp = "^(BEGINNER|INTERMEDIATE|ADVANCED|EXPERT)?$", message = "Niveau invalide (BEGINNER, INTERMEDIATE, ADVANCED, EXPERT)")
    private String level; // BEGINNER, INTERMEDIATE, ADVANCED, EXPERT

    @Positive(message = "Les années d'expérience doivent être un nombre positif")
    @Max(value = 50, message = "Les années d'expérience ne peuvent pas dépasser 50")
    private Integer yearsOfExperience;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}

