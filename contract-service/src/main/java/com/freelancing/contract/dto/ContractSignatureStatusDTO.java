package com.freelancing.contract.dto;

import lombok.Builder;
import lombok.Data;

import java.util.Date;

@Data
@Builder
public class ContractSignatureStatusDTO {
    private Long contractId;
    private Long clientId;
    private boolean clientSigned;
    private Date clientSignedAt;
    private String clientSignatureHash;
}
