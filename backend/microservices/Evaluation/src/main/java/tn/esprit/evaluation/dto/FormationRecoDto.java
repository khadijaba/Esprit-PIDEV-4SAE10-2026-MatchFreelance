package tn.esprit.evaluation.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FormationRecoDto {
    private Long id;
    private String titre;
    private String typeFormation;
    private String niveau;
    private String statut;
    private String dateDebut;
    private String dateFin;
    /** Lien direct vers la page formation côté plateforme/front. */
    private String lienDirect;
}

