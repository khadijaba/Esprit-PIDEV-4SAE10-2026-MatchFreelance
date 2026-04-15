package tn.esprit.evaluation.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "examen_adaptatif_etape")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExamenAdaptatifEtape {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id", nullable = false)
    private ExamenAdaptatifSession session;

    @Column(name = "question_id", nullable = false)
    private Long questionId;

    @Column(name = "reponse_choisie", nullable = false, length = 1)
    private String reponseChoisie;

    @Column(name = "correct", nullable = false)
    private boolean correct;

    @Column(nullable = false)
    private int ordre;
}
