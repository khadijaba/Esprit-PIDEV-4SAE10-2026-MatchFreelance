package tn.esprit.evaluation.dto;

import lombok.*;
import tn.esprit.evaluation.entity.PassageExamen;

import java.time.LocalDateTime;

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
    /** Certificat auto-généré lorsque resultat == REUSSI (renvoyé directement dans la réponse). */
    private CertificatDto certificat;

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
