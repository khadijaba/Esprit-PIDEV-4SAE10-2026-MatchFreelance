package com.freelancing.candidature.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CandidatureRequestDTO {

    @NotNull
    private Long projectId;

    @NotNull
    private Long freelancerId;

    @Size(max = 2000)
    private String message;

    @NotNull
    @Positive
    private Double proposedBudget;
}
