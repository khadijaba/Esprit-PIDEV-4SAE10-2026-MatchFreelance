package tn.esprit.evaluation.dto;

import lombok.*;
import tn.esprit.evaluation.entity.Question;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuestionDto {

    private Long id;
    private Long examenId;
    private Integer ordre;
    private String enonce;  // optionnel en création, défaut "Question N" côté service
    private String optionA;
    private String optionB;
    private String optionC;
    private String optionD;
    private String bonneReponse; // A, B, C ou D (défaut A côté service)

    public static QuestionDto fromEntity(Question q) {
        return QuestionDto.builder()
                .id(q.getId())
                .examenId(q.getExamen().getId())
                .ordre(q.getOrdre())
                .enonce(q.getEnonce())
                .optionA(q.getOptionA())
                .optionB(q.getOptionB())
                .optionC(q.getOptionC())
                .optionD(q.getOptionD())
                .bonneReponse(q.getBonneReponse())
                .build();
    }
}
