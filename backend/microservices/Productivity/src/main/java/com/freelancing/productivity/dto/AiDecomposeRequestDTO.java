package com.freelancing.productivity.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AiDecomposeRequestDTO {
    @NotBlank
    @Size(max = 2000)
    private String goalText;

    @Min(2)
    @Max(12)
    private Integer maxSteps;
}

