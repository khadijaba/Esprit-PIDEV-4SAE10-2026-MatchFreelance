package com.freelancing.contract.dto;

import com.freelancing.contract.enums.ContractStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ContractRequestDTO {

    @NotNull
    private Long projectId;

    @NotNull
    private Long freelancerId;

    @NotNull
    private Long clientId;

    @Size(max = 4000)
    private String terms;

    private Double proposedBudget;

    @Size(max = 2000)
    private String applicationMessage;

    private ContractStatus status;

    private Date startDate;

    private Date endDate;
}
