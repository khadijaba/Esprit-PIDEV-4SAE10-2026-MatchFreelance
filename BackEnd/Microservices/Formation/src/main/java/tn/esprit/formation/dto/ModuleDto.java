package tn.esprit.formation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.*;
import tn.esprit.formation.entity.Module;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ModuleDto {

    private Long id;
    @NotBlank
    private String titre;
    private String description;
    @NotNull
    @Positive
    private Integer dureeMinutes;
    @NotNull
    @PositiveOrZero
    @Builder.Default
    private Integer ordre = 0;
    @NotNull
    private Long formationId;

    public static ModuleDto fromEntity(Module m) {
        if (m == null) return null;
        return ModuleDto.builder()
                .id(m.getId())
                .titre(m.getTitre())
                .description(m.getDescription())
                .dureeMinutes(m.getDureeMinutes())
                .ordre(m.getOrdre() != null ? m.getOrdre() : 0)
                .formationId(m.getFormation() != null ? m.getFormation().getId() : null)
                .build();
    }
}
