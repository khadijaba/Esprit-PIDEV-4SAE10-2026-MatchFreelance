package tn.esprit.evaluation.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import tn.esprit.evaluation.domain.TypeParcours;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DemarrerAdaptatifRequest {

    @NotNull
    private Long freelancerId;

    @Builder.Default
    private TypeParcours typeParcours = TypeParcours.STANDARD;

    /** CERTIFIANT (défaut) ou ENTRAINEMENT. */
    @Builder.Default
    private ReponseExamenRequest.ModePassage mode = ReponseExamenRequest.ModePassage.CERTIFIANT;
}
