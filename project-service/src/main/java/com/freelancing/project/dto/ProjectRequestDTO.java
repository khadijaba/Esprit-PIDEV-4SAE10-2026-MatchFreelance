package com.freelancing.project.dto;

import com.freelancing.project.enums.ProjectStatus;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProjectRequestDTO {

    @NotBlank(message = "Title is required")
    @Size(min = 3, max = 255)
    private String title;

    @NotBlank(message = "Description is required")
    @Size(max = 2000)
    private String description;

    @NotNull(message = "Min budget is required")
    @Positive(message = "Min budget must be positive")
    private Double minBudget;

    @NotNull(message = "Max budget is required")
    @Positive(message = "Max budget must be positive")
    private Double maxBudget;

    @NotNull
    @Positive
    private Integer duration;

    private ProjectStatus status;
    private Long clientId;

    @AssertTrue(message = "Max budget must be greater than or equal to min budget")
    public boolean isValidBudgetRange() {
        return minBudget == null || maxBudget == null || maxBudget >= minBudget;
    }
}
