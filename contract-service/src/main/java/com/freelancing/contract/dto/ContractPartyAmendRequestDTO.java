package com.freelancing.contract.dto;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * Party-scoped update: set exactly one of {@code actorClientId} or {@code actorFreelancerId}.
 * Client may change terms, budget, dates, application message.
 * Freelancer may change application message and terms (negotiation).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ContractPartyAmendRequestDTO {

    private Long actorClientId;
    private Long actorFreelancerId;

    @Size(max = 4000)
    private String terms;

    private Double proposedBudget;

    private Date startDate;
    private Date endDate;

    @Size(max = 2000)
    private String applicationMessage;
}
