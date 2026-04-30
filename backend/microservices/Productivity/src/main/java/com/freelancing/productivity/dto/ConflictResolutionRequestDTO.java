package com.freelancing.productivity.dto;

import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class ConflictResolutionRequestDTO {
    @Min(1)
    private Long firstTaskId;

    @Min(1)
    private Long secondTaskId;
}

