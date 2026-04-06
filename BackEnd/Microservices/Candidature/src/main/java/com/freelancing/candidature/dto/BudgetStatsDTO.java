package com.freelancing.candidature.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Budget analytics for candidatures on a project (mean, median, percentiles, recommended range).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BudgetStatsDTO {

    private Long projectId;
    private int candidatureCount;
    private Double minProposedBudget;
    private Double maxProposedBudget;
    private Double averageProposedBudget;
    private Double medianProposedBudget;
    private Double percentile25;
    private Double percentile75;
    /** Recommended budget range (e.g. P25–P75) for new applicants. */
    private Double recommendedMin;
    private Double recommendedMax;
    /** Standard deviation of proposed budgets. */
    private Double standardDeviation;
}
