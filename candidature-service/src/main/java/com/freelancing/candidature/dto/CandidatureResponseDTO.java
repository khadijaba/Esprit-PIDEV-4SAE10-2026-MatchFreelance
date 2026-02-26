package com.freelancing.candidature.dto;

import com.freelancing.candidature.enums.CandidatureStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CandidatureResponseDTO {

    private Long id;
    private Long projectId;
    private Long freelancerId;
    private String message;
    private Double proposedBudget;
    private CandidatureStatus status;
    private Date createdAt;
}
