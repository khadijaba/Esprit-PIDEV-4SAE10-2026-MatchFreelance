package com.freelancing.contract.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RespondExtraBudgetRequestDTO {

    private boolean accept;

    @jakarta.validation.constraints.NotNull(message = "Client ID is required")
    private Long clientId;
}
