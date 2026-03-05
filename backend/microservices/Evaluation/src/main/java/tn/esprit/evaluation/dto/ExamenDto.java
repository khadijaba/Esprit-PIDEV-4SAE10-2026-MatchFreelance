package tn.esprit.evaluation.dto;

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
    @NotNull
    private Long formationId;
    @NotBlank
    private String titre;
    private String description;
    @NotNull
    @PositiveOrZero
    @Builder.Default
    private Integer seuilReussi = 60;

    @Builder.Default
    private List<QuestionDto> questions = new ArrayList<>();

    public static ExamenDto fromEntity(Examen e) {
        return ExamenDto.builder()
                .id(e.getId())
                .formationId(e.getFormationId())
                .titre(e.getTitre())
                .description(e.getDescription())
                .seuilReussi(e.getSeuilReussi())
                .questions(e.getQuestions() != null
                        ? e.getQuestions().stream().map(q -> QuestionDto.fromEntity(q, e.getId())).collect(Collectors.toList())
                        : new ArrayList<>())
                .build();
    }
}
