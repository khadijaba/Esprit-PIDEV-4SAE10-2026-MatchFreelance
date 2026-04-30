package com.freelancing.productivity.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.Instant;

@Data
public class TodoItemCreateRequestDTO {

    @NotBlank
    @Size(max = 250)
    private String title;

    private Instant dueAt;
}

