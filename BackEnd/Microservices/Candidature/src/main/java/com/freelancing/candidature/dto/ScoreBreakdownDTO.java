package com.freelancing.candidature.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Breakdown of the composite candidature ranking score by factor.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ScoreBreakdownDTO {

    /** Weighted contribution from AI match score (0–100). */
    private Double aiMatchContribution;
    /** Weighted contribution from budget competitiveness (0–100). */
    private Double budgetCompetitivenessContribution;
    /** Weighted contribution from response speed / application order (0–100). */
    private Double responseSpeedContribution;
    /** Weighted contribution from proposal quality (message length, clarity) (0–100). */
    private Double proposalQualityContribution;
    /** Weighted contribution from pitch analyzer (match to project skills, experience, communication) (0–100). */
    private Double pitchMatchContribution;
    /** Weighted contribution from chat communication (how freelancer responds in contract chats) (0–100). */
    private Double chatCommunicationContribution;
    /** Weights used: ai, budget, speed, quality, pitch, chat. */
    private String formulaDescription;
}
