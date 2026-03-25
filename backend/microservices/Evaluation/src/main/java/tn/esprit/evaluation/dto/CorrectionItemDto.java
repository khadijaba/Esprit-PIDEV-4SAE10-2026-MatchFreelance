package tn.esprit.evaluation.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CorrectionItemDto {
    private Integer ordre;
    private String enonce;
    private String reponseChoisie;  // A/B/C/D
    private String bonneReponse;    // A/B/C/D
    private boolean correct;
}

