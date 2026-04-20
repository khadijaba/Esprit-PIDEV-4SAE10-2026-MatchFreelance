package com.freelancing.candidature.dto;

import com.freelancing.candidature.enums.InterviewStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InterviewResponseDTO {

    private Long id;
    private Long candidatureId;
    private Date scheduledAt;
    private InterviewStatus status;
    private String notes;
}
