package com.freelancing.interview.dto;

import com.freelancing.interview.enums.MeetingMode;
import com.freelancing.interview.validation.ValidInterviewCreateRequest;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ValidInterviewCreateRequest
public class InterviewCreateRequestDTO {

    @Positive(message = "candidatureId must be positive")
    private Long candidatureId;

    @Positive(message = "projectId must be positive")
    private Long projectId;

    @NotNull(message = "freelancerId is required")
    @Positive(message = "freelancerId must be positive")
    private Long freelancerId;

    @NotNull(message = "ownerId is required")
    @Positive(message = "ownerId must be positive")
    private Long ownerId;

    /** Either provide slotId OR (startAt + endAt). */
    @Positive(message = "slotId must be positive")
    private Long slotId;
    private Instant startAt;
    private Instant endAt;

    /**
     * Optional: duration in minutes when creating from a slot.
     * If provided, endAt will be slot.startAt + durationMinutes (must not exceed slot end).
     */
    @Positive(message = "durationMinutes must be positive")
    private Integer durationMinutes;

    private MeetingMode mode;

    @Size(max = 2000, message = "meetingUrl must not exceed 2000 characters")
    private String meetingUrl;

    @Size(max = 500, message = "addressLine must not exceed 500 characters")
    private String addressLine;

    @Size(max = 200, message = "city must not exceed 200 characters")
    private String city;

    private Double lat;
    private Double lng;

    @Size(max = 2000, message = "notes must not exceed 2000 characters")
    private String notes;
}

