package com.freelancing.productivity.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.Instant;

@Data
public class TodoItemUpdateRequestDTO {

    @Size(max = 250)
    private String title;

    private Boolean done;

    private Instant dueAt;

    @Min(0)
    private Integer positionIndex;
}

