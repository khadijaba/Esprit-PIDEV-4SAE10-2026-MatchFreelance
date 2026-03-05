package com.freelancing.interview.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TopFreelancerInInterviewsDTO {
    private Long freelancerId;
    private double combinedScore;
    private double reliabilityScore;
    private Double averageReviewScore; // null if no reviews
    private long reviewCount;
    private int completedCount;
    private int noShowCount;
    private int cancelledCount;
}
