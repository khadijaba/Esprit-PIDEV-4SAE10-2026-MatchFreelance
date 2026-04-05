package com.freelancing.interview.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AvailabilitySlotResponseDTO {
    private Long id;
    private Long freelancerId;
    private Instant startAt;
    private Instant endAt;
    private boolean booked;
    private Long bookedInterviewId;
}

