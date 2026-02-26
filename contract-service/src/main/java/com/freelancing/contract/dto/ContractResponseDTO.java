package com.freelancing.contract.dto;

import com.freelancing.contract.enums.ContractStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ContractResponseDTO {

    private Long id;
    private Long projectId;
    private Long freelancerId;
    private Long clientId;
    private String terms;
    private Double proposedBudget;
    private String applicationMessage;
    private ContractStatus status;
    private Date startDate;
    private Date endDate;
    private Date createdAt;
}
