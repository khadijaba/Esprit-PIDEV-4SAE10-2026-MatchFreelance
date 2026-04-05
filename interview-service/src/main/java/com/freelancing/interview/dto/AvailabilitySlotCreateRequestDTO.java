package com.freelancing.interview.dto;

import com.freelancing.interview.validation.ValidSlotTimeRange;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ValidSlotTimeRange
public class AvailabilitySlotCreateRequestDTO {
    @NotNull(message = "startAt is required")
    private Instant startAt;

    @NotNull(message = "endAt is required")
    private Instant endAt;
}

