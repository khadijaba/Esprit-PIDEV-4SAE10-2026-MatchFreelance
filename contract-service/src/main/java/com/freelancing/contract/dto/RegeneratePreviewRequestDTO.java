package com.freelancing.contract.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegeneratePreviewRequestDTO {
    private String feedback;
    private String designStyle; // "modern", "bold", "corporate"
}
