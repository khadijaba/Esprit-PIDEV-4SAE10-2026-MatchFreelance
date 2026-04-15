package com.freelancing.contract.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProposeExtraBudgetRequestDTO {

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be positive")
    private Double amount;

    @Size(max = 1000)
    private String reason;

    @NotNull(message = "Freelancer ID is required")
    private Long freelancerId;
}
