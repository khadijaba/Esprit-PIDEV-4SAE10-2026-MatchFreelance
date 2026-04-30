package com.freelancing.productivity.dto;

import com.freelancing.productivity.enums.ProductivityPriority;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.Instant;

@Data
public class TaskCreateRequestDTO {

    @NotBlank
    @Size(max = 180)
    private String title;

    @Size(max = 4000)
    private String description;

    private ProductivityPriority priority;

    @Min(5)
    @Max(720)
    private Integer plannedMinutes;

    @Min(0)
    @Max(1440)
    private Integer actualMinutes;

    private Long goalId;

    private Instant dueAt;
}
