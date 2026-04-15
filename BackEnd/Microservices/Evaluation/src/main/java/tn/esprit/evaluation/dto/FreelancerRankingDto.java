package tn.esprit.evaluation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Position d'un freelancer dans le classement global (résultats examens + certifications).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FreelancerRankingDto {

    private Long freelancerId;
    private Integer rank;
    private Integer globalScore;
    private Integer averageScore;
    private Integer successRate;
    private Integer certificationsCount;
    private Integer attemptsCount;
    private LocalDateTime lastAttemptAt;
}
