package com.freelancing.contract.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Exactly one of {@code clientId} or {@code freelancerId} must be set and must match the contract.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ContractCancelPartyRequestDTO {

    private Long clientId;
    private Long freelancerId;
}
