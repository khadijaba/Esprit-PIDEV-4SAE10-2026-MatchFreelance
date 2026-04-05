package tn.esprit.formation.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import tn.esprit.formation.entity.Inscription;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InscriptionDto {

    private Long id;
    @NotNull
    private Long freelancerId;
    @NotNull
    private Long formationId;
    private String formationTitre;
    private Inscription.StatutInscription statut;
    private LocalDateTime dateInscription;

    public static InscriptionDto fromEntity(Inscription i) {
        return InscriptionDto.builder()
                .id(i.getId())
                .freelancerId(i.getFreelancerId())
                .formationId(i.getFormation().getId())
                .formationTitre(i.getFormation() != null ? i.getFormation().getTitre() : null)
                .statut(i.getStatut())
                .dateInscription(i.getDateInscription())
                .build();
    }
}
