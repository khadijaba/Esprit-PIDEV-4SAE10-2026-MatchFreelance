package tn.esprit.evaluation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdaptatifRepondreRequest {

    @NotNull
    private Long questionId;

    @NotBlank
    private String reponse;
}
