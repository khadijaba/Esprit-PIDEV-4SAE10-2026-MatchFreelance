package tn.esprit.evaluation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AmenagementTempsUpsertRequest {
    /** Entre 1.0 et 3.0 (temps majoré). */
    private Double multiplicateurChrono;
    private String motif;
    private Boolean actif;
}
