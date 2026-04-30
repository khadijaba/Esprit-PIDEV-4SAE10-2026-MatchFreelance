package com.freelancing.productivity.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class DecisionLogCreateRequestDTO {
    private Long taskId;

    @NotBlank
    @Size(max = 80)
    private String decisionType;

    @NotBlank
    @Size(max = 4000)
    private String reason;
}

