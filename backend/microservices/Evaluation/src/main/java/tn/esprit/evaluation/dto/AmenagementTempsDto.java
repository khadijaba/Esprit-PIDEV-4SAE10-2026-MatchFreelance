package tn.esprit.evaluation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AmenagementTempsDto {
    private int secondesParQuestionBase;
    private double multiplicateurChrono;
    private int secondesEffectivesParQuestion;
}
