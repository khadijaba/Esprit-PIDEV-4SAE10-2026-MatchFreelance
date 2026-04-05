package com.freelancing.interview.dto;

import com.freelancing.interview.enums.InterviewStatus;
import com.freelancing.interview.enums.MeetingMode;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InterviewResponseDTO {
    private Long id;
    private Long candidatureId;
    private Long projectId;
    private Long freelancerId;
    private Long ownerId;
    private Long slotId;
    private Instant startAt;
    private Instant endAt;
    private MeetingMode mode;
    private String meetingUrl;
    private String addressLine;
    private String city;
    private Double lat;
    private Double lng;
    private InterviewStatus status;
    private String notes;
    private Instant createdAt;
}

