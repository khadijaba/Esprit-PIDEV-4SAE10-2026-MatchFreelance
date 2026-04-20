package com.freelancing.contract.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

/**
 * Local LLM (Ollama) contract briefing: narrative + actionable steps. Not legal or financial advice.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContractAiBriefingDTO {

    private Long contractId;
    private String summary;
    private String timelineRisk;
    private String chatTone;
    private List<String> suggestedNextSteps;
    /** Fixed platform disclaimer; also echoed from server for transparency. */
    private String disclaimer;
    private String model;
    private Instant generatedAt;
    /** True when Ollama returned invalid JSON and server filled minimal fields from heuristics. */
    private boolean fallback;
}
