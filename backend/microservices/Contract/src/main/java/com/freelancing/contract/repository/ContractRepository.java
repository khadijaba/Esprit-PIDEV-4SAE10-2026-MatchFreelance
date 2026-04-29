package com.freelancing.contract.repository;

import com.freelancing.contract.entity.Contract;
import com.freelancing.contract.enums.ContractStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ContractRepository extends JpaRepository<Contract, Long> {

    List<Contract> findByProjectId(Long projectId);

    List<Contract> findByProjectIdAndStatusIn(Long projectId, Collection<ContractStatus> statuses);

    List<Contract> findByFreelancerId(Long freelancerId);

    List<Contract> findByClientId(Long clientId);

    Optional<Contract> findFirstByProjectId(Long projectId);

    List<Contract> findByStatus(ContractStatus status);
}
