package com.freelancing.contract.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Financial summary for a contract: totals, platform fee, net amounts, and optional milestones.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FinancialSummaryDTO {

    private Long contractId;
    private Double baseBudget;
    private Double extraTasksBudget;
    private Double totalContractValue;
    private Double platformFeePercent;
    private Double platformFeeAmount;
    private Double freelancerNetAmount;
    private Double clientTotalAmount;
    private Double amountReleasableByProgress;
    private List<PaymentMilestoneDTO> paymentSchedule;
}
