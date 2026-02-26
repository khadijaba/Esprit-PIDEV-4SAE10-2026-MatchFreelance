package com.freelancing.interview.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AvailabilitySlotBatchCreateRequestDTO {

    @NotEmpty(message = "At least one slot is required")
    @Size(max = 200, message = "Batch cannot exceed 200 slots per request")
    @Valid
    private List<AvailabilitySlotCreateRequestDTO> slots;
}
