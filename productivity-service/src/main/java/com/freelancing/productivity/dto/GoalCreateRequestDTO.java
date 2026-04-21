package com.freelancing.productivity.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.Instant;

@Data
public class GoalCreateRequestDTO {
    @NotBlank
    @Size(max = 180)
    private String title;

    @Size(max = 2000)
    private String description;

    private Instant targetDate;
}

