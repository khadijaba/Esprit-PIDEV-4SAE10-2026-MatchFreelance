package tn.esprit.evaluation.dto;

import lombok.*;

/** Fiche / module de formation ciblé après erreurs sur des thèmes donnés. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ModuleRevisionDto {

    private Long id;
    private String titre;
    private String description;
    private Integer ordre;
    private Integer dureeMinutes;
    /** Pourquoi ce module est proposé (thème, correspondance, etc.). */
    private String raison;
}
