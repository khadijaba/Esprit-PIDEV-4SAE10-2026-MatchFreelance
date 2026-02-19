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
    private String examenTitre;
    private Integer score;
    private LocalDateTime datePassage;
    private LocalDateTime dateDelivrance;

    public static CertificatDto fromEntity(Certificat c) {
        if (c == null || c.getPassageExamen() == null) return null;
        var p = c.getPassageExamen();
        return CertificatDto.builder()
                .id(c.getId())
                .numeroCertificat(c.getNumeroCertificat())
                .passageExamenId(p.getId())
                .freelancerId(p.getFreelancerId())
                .examenId(p.getExamen().getId())
                .examenTitre(p.getExamen().getTitre())
                .score(p.getScore())
                .datePassage(p.getDatePassage())
                .dateDelivrance(c.getDateDelivrance())
                .build();
    }
}
