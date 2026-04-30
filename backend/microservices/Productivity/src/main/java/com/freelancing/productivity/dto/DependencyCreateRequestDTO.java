package com.freelancing.productivity.dto;

import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class DependencyCreateRequestDTO {
    @Min(1)
    private Long predecessorTaskId;

    @Min(1)
    private Long successorTaskId;
}

