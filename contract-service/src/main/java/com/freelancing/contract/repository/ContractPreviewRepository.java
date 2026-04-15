package com.freelancing.contract.repository;

import com.freelancing.contract.entity.ContractPreview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ContractPreviewRepository extends JpaRepository<ContractPreview, Long> {
    
    List<ContractPreview> findByContractIdOrderByVersionDesc(Long contractId);
    
    Optional<ContractPreview> findByContractIdAndId(Long contractId, Long previewId);
    
    Optional<ContractPreview> findFirstByContractIdOrderByVersionDesc(Long contractId);
}
