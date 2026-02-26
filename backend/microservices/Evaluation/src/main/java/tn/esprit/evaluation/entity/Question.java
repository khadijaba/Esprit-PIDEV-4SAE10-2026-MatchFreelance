package tn.esprit.evaluation.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

/**
 * Question QCM d'un examen. Une seule bonne réponse (A, B, C ou D).
 */
@Entity
@Table(name = "question")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Question {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "examen_id", nullable = false)
    private Examen examen;

    @Column(nullable = false)
    private Integer ordre;

    @NotBlank
    @Column(nullable = false, length = 1000)
    private String enonce;

    @Column(name = "option_a", length = 500)
    private String optionA;

    @Column(name = "option_b", length = 500)
    private String optionB;

    @Column(name = "option_c", length = 500)
    private String optionC;

    @Column(name = "option_d", length = 500)
    private String optionD;

    /** Bonne réponse : A, B, C ou D */
    @Column(name = "bonne_reponse", nullable = false, length = 1)
    private String bonneReponse;
}
