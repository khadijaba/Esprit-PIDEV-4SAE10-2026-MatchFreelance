package tn.esprit.evaluation.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.*;
import tn.esprit.evaluation.entity.Examen;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExamenDto {

    private Long id;
    @NotNull(message = "formationId est requis")
    private Long formationId;
    @NotBlank(message = "Le titre est requis")
    @jakarta.validation.constraints.Size(min = 1, max = 255)
    private String titre;
    @jakarta.validation.constraints.Size(max = 1000)
    private String description;
    @NotNull
    @PositiveOrZero
    @Max(100)
    @Builder.Default
    private Integer seuilReussi = 60;

    @Builder.Default
    private List<QuestionDto> questions = new ArrayList<>();

    public static ExamenDto fromEntity(Examen e) {
        final Long examenId = e != null ? e.getId() : null;
        return ExamenDto.builder()
                .id(e.getId())
                .formationId(e.getFormationId())
                .titre(e.getTitre())
                .description(e.getDescription())
                .seuilReussi(e.getSeuilReussi())
                .questions(e.getQuestions() != null
                        ? e.getQuestions().stream().map(q -> QuestionDto.fromEntity(q, examenId)).collect(Collectors.toList())
                        : new ArrayList<>())
                .build();
    }
}
