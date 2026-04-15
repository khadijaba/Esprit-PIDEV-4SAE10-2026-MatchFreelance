package com.freelancing.contract.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

/**
 * Local LLM analysis of a proposed extra-budget request (informational only; not legal or financial advice).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExtraBudgetAiAnalysisDTO {

    private Long contractId;
    /** NEEDED, QUESTIONABLE, or UNCLEAR */
    private String needAssessment;
    private String needRationale;
    /** BELOW_MARKET, FAIR, HIGH, or UNCLEAR */
    private String priceAssessment;
    private String priceRationale;
    /** ALIGNED, NEUTRAL, MISALIGNED, or UNCLEAR */
    private String projectFit;
    private String projectFitRationale;
    private List<String> risksOrConcerns;
    private List<String> negotiationTips;
    private String overallSummary;
    private String disclaimer;
    private String model;
    private Instant generatedAt;
    private boolean fallback;
}
