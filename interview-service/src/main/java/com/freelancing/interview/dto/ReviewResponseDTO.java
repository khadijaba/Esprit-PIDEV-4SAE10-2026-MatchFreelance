package com.freelancing.interview.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReviewResponseDTO {
    private Long id;
    private Long interviewId;
    private Long reviewerId;
    private Long revieweeId;
    private Integer score;
    private String comment;
    private Instant createdAt;
}
