package com.freelancing.productivity.dto;

import lombok.Data;

@Data
public class ContextSuggestionDTO {
    private String category;
    private String message;
    private double confidence;
}

