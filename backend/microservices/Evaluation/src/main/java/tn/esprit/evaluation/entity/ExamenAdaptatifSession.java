package tn.esprit.evaluation.entity;

import jakarta.persistence.*;
import lombok.*;
import tn.esprit.evaluation.domain.TypeParcours;
import tn.esprit.evaluation.dto.ReponseExamenRequest;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Session d’examen adaptatif : une question à la fois, difficulté ajustée selon les réponses.
 */
@Entity
@Table(name = "examen_adaptatif_session")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExamenAdaptatifSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "token", nullable = false, unique = true, length = 40)
    private String token;

    @Column(name = "examen_id", nullable = false)
    private Long examenId;

    @Column(name = "freelancer_id", nullable = false)
    private Long freelancerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type_parcours", nullable = false, length = 20)
    private TypeParcours typeParcours;

    @Enumerated(EnumType.STRING)
    @Column(name = "mode_passage", nullable = false, length = 20)
    private ReponseExamenRequest.ModePassage modePassage;

    /**
     * Rang de difficulté cible pour la prochaine question : 1=FACILE, 2=MOYEN, 3=DIFFICILE.
     */
    @Column(name = "difficulte_cible_rang", nullable = false)
    @Builder.Default
    private int difficulteCibleRang = 2;

    @Column(name = "question_courante_id")
    private Long questionCouranteId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private StatutSession statut = StatutSession.EN_COURS;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("ordre ASC")
    @Builder.Default
    private List<ExamenAdaptatifEtape> etapes = new ArrayList<>();

    public enum StatutSession {
        EN_COURS,
        TERMINEE,
        ANNULEE
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (expiresAt == null) {
            expiresAt = createdAt.plusHours(4);
        }
    }
}
