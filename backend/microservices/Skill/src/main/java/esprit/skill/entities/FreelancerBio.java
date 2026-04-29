package esprit.skill.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "freelancer_bio", uniqueConstraints = @UniqueConstraint(columnNames = "freelancer_id"))
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FreelancerBio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "L'identifiant du freelancer est obligatoire")
    @Positive(message = "L'identifiant du freelancer doit être positif")
    @Column(name = "freelancer_id", nullable = false, unique = true)
    private Long freelancerId;

    @Size(max = 2000, message = "La bio ne peut pas dépasser 2000 caractères")
    @Column(columnDefinition = "TEXT")
    private String bio;
}
