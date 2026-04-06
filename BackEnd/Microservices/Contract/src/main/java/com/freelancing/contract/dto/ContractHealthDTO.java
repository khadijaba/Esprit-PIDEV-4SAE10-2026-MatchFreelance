package com.freelancing.contract.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Contract health/risk score and flags (on track, behind schedule, pending extra, etc.).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ContractHealthDTO {

    private Long contractId;
    private int healthScore;
    private String healthLevel;
    private List<String> flags;
    private String timelineStatus;
    private Double progressVsExpectedRatio;
}
