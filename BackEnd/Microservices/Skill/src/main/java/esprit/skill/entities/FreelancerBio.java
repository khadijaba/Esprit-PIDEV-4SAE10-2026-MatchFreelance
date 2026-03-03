package esprit.skill.entities;

import jakarta.persistence.*;
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

    @Column(name = "freelancer_id", nullable = false, unique = true)
    private Long freelancerId;

    @Column(columnDefinition = "TEXT")
    private String bio;
}
