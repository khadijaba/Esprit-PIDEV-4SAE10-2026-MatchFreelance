package esprit.project.dto.candidature;

import lombok.Data;

import java.util.Date;

/** Sous-ensemble des champs exposés par le microservice Candidature (Feign). */
@Data
public class CandidatureSummaryDto {
    private Long id;
    private Long projectId;
    private Long freelancerId;
    private String status;
    private Date createdAt;
}
