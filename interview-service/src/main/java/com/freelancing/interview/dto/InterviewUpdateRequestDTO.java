package com.freelancing.interview.dto;

import com.freelancing.interview.enums.InterviewStatus;
import com.freelancing.interview.enums.MeetingMode;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InterviewUpdateRequestDTO {
    private Instant startAt;
    private Instant endAt;
    private MeetingMode mode;

    @Size(max = 2000, message = "meetingUrl must not exceed 2000 characters")
    private String meetingUrl;

    @Size(max = 500, message = "addressLine must not exceed 500 characters")
    private String addressLine;

    @Size(max = 200, message = "city must not exceed 200 characters")
    private String city;

    private Double lat;
    private Double lng;

    private InterviewStatus status;

    @Size(max = 2000, message = "notes must not exceed 2000 characters")
    private String notes;

    @AssertTrue(message = "endAt must be after startAt when both are provided")
    public boolean isTimeRangeValid() {
        if (startAt == null || endAt == null) return true;
        return endAt.isAfter(startAt);
    }
}

