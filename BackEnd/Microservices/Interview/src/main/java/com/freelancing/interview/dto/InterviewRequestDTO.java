package com.freelancing.interview.dto;

import com.freelancing.interview.enums.InterviewStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InterviewRequestDTO {

    /** ISO-8601 depuis le front (ex. {@code 2026-04-15T10:00:00.000Z}). */
    @NotNull(message = "Scheduled date/time is required")
    private Instant scheduledAt;

    private InterviewStatus status;

    @Size(max = 2000)
    private String notes;
}
