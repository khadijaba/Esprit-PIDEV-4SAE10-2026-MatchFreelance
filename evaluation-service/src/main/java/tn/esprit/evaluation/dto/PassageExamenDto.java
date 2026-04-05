package tn.esprit.evaluation.dto;

import lombok.*;
import tn.esprit.evaluation.entity.PassageExamen;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PassageExamenDto {

    private Long id;
    private Long freelancerId;
    private Long examenId;
    private String examenTitre;
    private Integer score;
    private PassageExamen.ResultatExamen resultat;
    private LocalDateTime datePassage;
    private ReponseExamenRequest.ModePassage mode;
    private Integer totalQuestions;
    private Integer bonnesReponses;
    /** Certificat auto-généré lorsque resultat == REUSSI (renvoyé directement dans la réponse). */
    private CertificatDto certificat;
    /** Correction détaillée (uniquement en mode ENTRAINEMENT). */
    private List<CorrectionItemDto> correction;
    /** Analyse automatique: axes d'amélioration détectés sur les erreurs. */
    private List<String> analyseErreurs;
    /** Message intelligent personnalisé selon le résultat. */
    private String messageFeedback;
    /** Feedback : formations recommandées (en lien avec Formation). */
    private List<FormationRecoDto> formationsRecommandees;

    public static PassageExamenDto fromEntity(PassageExamen p) {
        return PassageExamenDto.builder()
                .id(p.getId())
                .freelancerId(p.getFreelancerId())
                .examenId(p.getExamen().getId())
                .examenTitre(p.getExamen().getTitre()) 
                .score(p.getScore())
                .resultat(p.getResultat())
                .datePassage(p.getDatePassage())
                .build();
    }
}
