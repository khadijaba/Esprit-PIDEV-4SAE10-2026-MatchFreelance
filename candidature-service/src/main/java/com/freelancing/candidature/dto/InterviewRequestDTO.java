package com.freelancing.candidature.dto;

import com.freelancing.candidature.enums.InterviewStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InterviewRequestDTO {

    @NotNull(message = "Scheduled date/time is required")
    private Date scheduledAt;

    private InterviewStatus status;

    @Size(max = 2000)
    private String notes;
}
