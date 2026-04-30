package com.freelancing.productivity.dto;

import lombok.Data;

import java.util.List;

@Data
public class AiDecomposeResponseDTO {
    private String inputGoal;
    private List<String> suggestedSteps;
    private String rationale;
    private String aiSource;
}

