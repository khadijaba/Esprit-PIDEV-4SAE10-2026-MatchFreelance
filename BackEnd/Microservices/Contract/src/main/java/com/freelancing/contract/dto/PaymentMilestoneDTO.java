package com.freelancing.contract.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A single payment milestone (e.g. 25% at 25% progress).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentMilestoneDTO {

    private int milestoneIndex;
    private int progressPercentTrigger;
    private Double amount;
    private boolean released;
    private String statusDescription;
}
