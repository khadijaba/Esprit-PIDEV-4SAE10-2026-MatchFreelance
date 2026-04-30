package com.freelancing.productivity.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class TodoListCreateRequestDTO {

    @NotBlank
    @Size(max = 120)
    private String name;
}

