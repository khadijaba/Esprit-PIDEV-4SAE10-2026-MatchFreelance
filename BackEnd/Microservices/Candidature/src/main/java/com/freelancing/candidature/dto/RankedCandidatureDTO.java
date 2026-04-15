package com.freelancing.candidature.dto;

import com.freelancing.candidature.enums.CandidatureStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * DTO for a candidature with composite ranking score and score breakdown.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RankedCandidatureDTO {

    private Long id;
    private Long projectId;
    private Long freelancerId;
    private String freelancerName;
    private String message;
    private Double proposedBudget;
    private Double extraTasksBudget;
    private CandidatureStatus status;
    private Date createdAt;
    private Double aiMatchScore;
    private String aiInsights;

    /** Composite ranking score 0–100 used for shortlisting. */
    private Double compositeScore;
    /** Human-readable breakdown of how the composite score was computed. */
    private ScoreBreakdownDTO scoreBreakdown;
    /** Rank position among candidatures for this project (1-based). */
    private Integer rank;
}
