package esprit.skill.entities;

import jakarta.persistence.*;
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

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SkillCategory category;

    /**
     * ID du freelancer (utilisateur). Pas d'entite Freelancer dans ce microservice :
     * le User/Freelancer est dans un autre microservice (camarade). On stocke uniquement l'ID.
     */
    @Column(nullable = false)
    private Long freelancerId;

    private String level; // BEGINNER, INTERMEDIATE, ADVANCED, EXPERT

    private Integer yearsOfExperience;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}

