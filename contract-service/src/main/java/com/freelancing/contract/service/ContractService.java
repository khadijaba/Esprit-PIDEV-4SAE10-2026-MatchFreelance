package com.freelancing.contract.service;

import com.freelancing.contract.dto.ContractRequestDTO;
import com.freelancing.contract.dto.ContractResponseDTO;
import com.freelancing.contract.entity.Contract;
import com.freelancing.contract.enums.ContractStatus;
import com.freelancing.contract.repository.ContractRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ContractService {

    private final ContractRepository contractRepository;

    @Transactional(readOnly = true)
    public List<ContractResponseDTO> getAllContracts() {
        return contractRepository.findAll().stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ContractResponseDTO getContractById(Long id) {
        Contract c = contractRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Contract not found with id: " + id));
        return toResponseDTO(c);
    }

    @Transactional(readOnly = true)
    public List<ContractResponseDTO> getContractsByProjectId(Long projectId) {
        return contractRepository.findByProjectId(projectId).stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ContractResponseDTO> getContractsByFreelancerId(Long freelancerId) {
        return contractRepository.findByFreelancerId(freelancerId).stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ContractResponseDTO> getContractsByClientId(Long clientId) {
        return contractRepository.findByClientId(clientId).stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public ContractResponseDTO createContract(ContractRequestDTO dto) {
        Contract c = new Contract();
        c.setProjectId(dto.getProjectId());
        c.setFreelancerId(dto.getFreelancerId());
        c.setClientId(dto.getClientId());
        c.setTerms(dto.getTerms());
        c.setProposedBudget(dto.getProposedBudget());
        c.setApplicationMessage(dto.getApplicationMessage());
        c.setStatus(dto.getStatus() != null ? dto.getStatus() : ContractStatus.DRAFT);
        c.setStartDate(dto.getStartDate());
        c.setEndDate(dto.getEndDate());
        Contract saved = contractRepository.save(c);
        return toResponseDTO(saved);
    }

    @Transactional
    public ContractResponseDTO updateContract(Long id, ContractRequestDTO dto) {
        Contract c = contractRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Contract not found with id: " + id));
        if (dto.getTerms() != null) c.setTerms(dto.getTerms());
        if (dto.getProposedBudget() != null) c.setProposedBudget(dto.getProposedBudget());
        if (dto.getApplicationMessage() != null) c.setApplicationMessage(dto.getApplicationMessage());
        if (dto.getStartDate() != null) c.setStartDate(dto.getStartDate());
        if (dto.getEndDate() != null) c.setEndDate(dto.getEndDate());
        if (dto.getStatus() != null) c.setStatus(dto.getStatus());
        return toResponseDTO(contractRepository.save(c));
    }

    @Transactional
    public ContractResponseDTO markAsCompleted(Long id) {
        Contract c = contractRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Contract not found with id: " + id));
        if (c.getStatus() != ContractStatus.ACTIVE) {
            throw new RuntimeException("Only ACTIVE contracts can be marked as paid");
        }
        c.setStatus(ContractStatus.COMPLETED);
        return toResponseDTO(contractRepository.save(c));
    }

    @Transactional
    public ContractResponseDTO markAsCancelled(Long id) {
        Contract c = contractRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Contract not found with id: " + id));
        if (c.getStatus() != ContractStatus.ACTIVE) {
            throw new RuntimeException("Only ACTIVE contracts can be cancelled");
        }
        c.setStatus(ContractStatus.CANCELLED);
        return toResponseDTO(contractRepository.save(c));
    }

    @Transactional
    public void deleteContract(Long id) {
        if (!contractRepository.existsById(id)) {
            throw new RuntimeException("Contract not found with id: " + id);
        }
        contractRepository.deleteById(id);
    }

    private ContractResponseDTO toResponseDTO(Contract c) {
        ContractResponseDTO dto = new ContractResponseDTO();
        dto.setId(c.getId());
        dto.setProjectId(c.getProjectId());
        dto.setFreelancerId(c.getFreelancerId());
        dto.setClientId(c.getClientId());
        dto.setTerms(c.getTerms());
        dto.setProposedBudget(c.getProposedBudget());
        dto.setApplicationMessage(c.getApplicationMessage());
        dto.setStatus(c.getStatus());
        dto.setStartDate(c.getStartDate());
        dto.setEndDate(c.getEndDate());
        dto.setCreatedAt(c.getCreatedAt());
        return dto;
    }
}
