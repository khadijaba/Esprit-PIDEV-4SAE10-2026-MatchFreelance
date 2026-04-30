package com.freelancing.productivity.dto;

import lombok.Data;

@Data
public class ConflictResolutionResponseDTO {
    private Long recommendedTaskId;
    private Long deferredTaskId;
    private double recommendedScore;
    private double deferredScore;
    private String rationale;
    private String aiSource;
}

