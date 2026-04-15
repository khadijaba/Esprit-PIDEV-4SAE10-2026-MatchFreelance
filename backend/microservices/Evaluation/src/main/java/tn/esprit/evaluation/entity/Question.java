package tn.esprit.evaluation.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import tn.esprit.evaluation.domain.NiveauDifficulteQuestion;

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

    /** Lot parcours : COMMUN (les deux), STANDARD ou RENFORCEMENT uniquement. */
    @Enumerated(EnumType.STRING)
    @Column(name = "parcours_inclusion", nullable = false, length = 20)
    @Builder.Default
    private ParcoursInclusion parcoursInclusion = ParcoursInclusion.COMMUN;

    /** Pour l’examen adaptatif : FACILE / MOYEN / DIFFICILE. */
    @Enumerated(EnumType.STRING)
    @Column(name = "niveau_difficulte", nullable = false, length = 20)
    @Builder.Default
    private NiveauDifficulteQuestion niveauDifficulte = NiveauDifficulteQuestion.MOYEN;

    /**
     * Thème pédagogique (ex. "SQL", "Angular") pour relier les erreurs à des modules Formation / fiches.
     */
    @Column(name = "theme", length = 120)
    private String theme;

    /**
     * Compétence métier évaluée par la question (ex. « Java », « Networking »). Si vide, le score par compétence
     * retombe sur {@link #theme} puis sur « Autres ».
     */
    @Column(name = "skill", length = 120)
    private String skill;

    /**
     * Explication pédagogique (pourquoi la bonne réponse est correcte) — affichée dans la correction détaillée.
     */
    @Column(name = "explication", length = 2000)
    private String explication;
}
