package tn.esprit.formation.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Inscription d'un freelancer à une formation.
 * freelancerId référence l'ID du freelancer dans le microservice Freelancer (appel via Gateway).
 */
@Entity
@Table(name = "inscription", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"formation_id", "freelancer_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Inscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Column(name = "freelancer_id", nullable = false)
    private Long freelancerId;  // ID dans le microservice Freelancer

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "formation_id", nullable = false)
    private Formation formation;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private StatutInscription statut = StatutInscription.EN_ATTENTE;

    @Column(updatable = false)
    @Builder.Default
    private LocalDateTime dateInscription = LocalDateTime.now();

    public enum StatutInscription {
        EN_ATTENTE,
        VALIDEE,
        REFUSEE,
        ANNULEE
    }
}
