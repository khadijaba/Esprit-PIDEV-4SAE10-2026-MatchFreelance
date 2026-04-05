package tn.esprit.evaluation.dto;

import lombok.*;
import tn.esprit.evaluation.entity.Certificat;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CertificatDto {

    private Long id;
    private String numeroCertificat;
    private Long passageExamenId;
    private Long freelancerId;
    private Long examenId;
    /** Formation liée à l'examen (pour filtrer les recommandations « autres »). */
    private Long formationId;
    private String examenTitre;
    private Integer score;
    /** Seuil de réussite de l'examen (ex: 60). Pour affichage sur le certificat. */
    private Integer seuilReussi;
    private LocalDateTime datePassage;
    private LocalDateTime dateDelivrance;

    public static CertificatDto fromEntity(Certificat c) {
        if (c == null || c.getPassageExamen() == null) return null;
        var p = c.getPassageExamen();
        var examen = p.getExamen();
        return CertificatDto.builder()
                .id(c.getId())
                .numeroCertificat(c.getNumeroCertificat())
                .passageExamenId(p.getId())
                .freelancerId(p.getFreelancerId())
                .examenId(examen != null ? examen.getId() : null)
                .formationId(examen != null ? examen.getFormationId() : null)
                .examenTitre(examen != null ? examen.getTitre() : null)
                .score(p.getScore())
                .seuilReussi(examen != null ? examen.getSeuilReussi() : 60)
                .datePassage(p.getDatePassage())
                .dateDelivrance(c.getDateDelivrance())
                .build();
    }
}
