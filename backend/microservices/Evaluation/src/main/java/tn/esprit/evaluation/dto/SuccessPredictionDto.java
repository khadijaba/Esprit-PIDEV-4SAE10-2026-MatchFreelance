package tn.esprit.evaluation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * Simulation de réussite avant passage certifiant.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SuccessPredictionDto {

    /** 0..100 */
    private int probabiliteReussite;
    private String niveauConfiance;
    private Integer scoreMoyenHistorique;
    private Integer tauxReussiteHistorique;
    private Integer tempsMoyenPreparationJours;
    private String recommandation;

    @Builder.Default
    private List<ModuleRevisionDto> modulesAvantCertifiant = new ArrayList<>();
}
