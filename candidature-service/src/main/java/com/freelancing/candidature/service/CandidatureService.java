package com.freelancing.candidature.service;

import com.freelancing.candidature.client.ContractClient;
import com.freelancing.candidature.client.ProjectClient;
import com.freelancing.candidature.dto.CandidatureRequestDTO;
import com.freelancing.candidature.dto.CandidatureResponseDTO;
import com.freelancing.candidature.entity.Candidature;
import com.freelancing.candidature.enums.CandidatureStatus;
import com.freelancing.candidature.repository.CandidatureRepository;
import com.freelancing.candidature.repository.CandidatureSpecifications;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CandidatureService {

    private final CandidatureRepository candidatureRepository;
    private final ProjectClient projectClient;
    private final ContractClient contractClient;

    @Transactional(readOnly = true)
    public List<CandidatureResponseDTO> getAllCandidatures() {
        return candidatureRepository.findAll().stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<CandidatureResponseDTO> getCandidaturesPage(Long projectId, Long freelancerId, CandidatureStatus status, Pageable pageable) {
        Specification<Candidature> spec = Specification.where(CandidatureSpecifications.hasProjectId(projectId))
                .and(CandidatureSpecifications.hasFreelancerId(freelancerId))
                .and(CandidatureSpecifications.hasStatus(status));

        return candidatureRepository.findAll(spec, pageable).map(this::toResponseDTO);
    }

    @Transactional(readOnly = true)
    public CandidatureResponseDTO getCandidatureById(Long id) {
        Candidature c = candidatureRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Candidature not found with id: " + id));
        return toResponseDTO(c);
    }

    @Transactional(readOnly = true)
    public List<CandidatureResponseDTO> getCandidaturesByProjectId(Long projectId) {
        return candidatureRepository.findByProjectId(projectId).stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<CandidatureResponseDTO> getCandidaturesByFreelancerId(Long freelancerId) {
        return candidatureRepository.findByFreelancerId(freelancerId).stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public CandidatureResponseDTO createCandidature(CandidatureRequestDTO dto) {
        ProjectClient.ProjectResponse project = projectClient.getProjectById(dto.getProjectId());
        if (project == null) {
            throw new RuntimeException("Project not found with id: " + dto.getProjectId());
        }
        if (!"OPEN".equals(project.getStatus())) {
            throw new RuntimeException("Project is not open for applications");
        }
        if (candidatureRepository.existsByProjectIdAndFreelancerId(dto.getProjectId(), dto.getFreelancerId())) {
            throw new RuntimeException("You have already applied to this project");
        }
        if (dto.getProposedBudget() == null || dto.getProposedBudget() < project.getMinBudget() || dto.getProposedBudget() > project.getMaxBudget()) {
            throw new RuntimeException("Proposed budget must be between " + project.getMinBudget() + " and " + project.getMaxBudget());
        }

        Candidature c = new Candidature();
        c.setProjectId(dto.getProjectId());
        c.setFreelancerId(dto.getFreelancerId());
        c.setMessage(dto.getMessage());
        c.setProposedBudget(dto.getProposedBudget());
        c.setStatus(CandidatureStatus.PENDING);
        Candidature saved = candidatureRepository.save(c);
        return toResponseDTO(saved);
    }

    @Transactional
    public CandidatureResponseDTO updateCandidature(Long id, CandidatureRequestDTO dto) {
        Candidature c = candidatureRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Candidature not found with id: " + id));
        c.setMessage(dto.getMessage());
        return toResponseDTO(candidatureRepository.save(c));
    }

    @Transactional
    public CandidatureResponseDTO acceptCandidature(Long id, Long clientId) {
        Candidature c = candidatureRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Candidature not found with id: " + id));

        if (c.getStatus() != CandidatureStatus.PENDING) {
            throw new RuntimeException("Candidature is not pending");
        }

        ProjectClient.ProjectResponse project = projectClient.getProjectById(c.getProjectId());
        if (project == null) {
            throw new RuntimeException("Project not found");
        }
        if (project.getClientId() != null && !project.getClientId().equals(clientId)) {
            throw new RuntimeException("Only the project owner can accept candidatures");
        }

        List<Candidature> alreadyAccepted = candidatureRepository.findByProjectIdAndStatus(c.getProjectId(), CandidatureStatus.ACCEPTED);
        if (!alreadyAccepted.isEmpty()) {
            throw new RuntimeException("A freelancer has already been accepted for this project");
        }

        List<Candidature> others = candidatureRepository.findByProjectIdAndStatus(c.getProjectId(), CandidatureStatus.PENDING);
        for (Candidature other : others) {
            if (!other.getId().equals(id)) {
                other.setStatus(CandidatureStatus.REJECTED);
                candidatureRepository.save(other);
            }
        }

        c.setStatus(CandidatureStatus.ACCEPTED);
        candidatureRepository.save(c);

        Long effectiveClientId = project.getClientId() != null ? project.getClientId() : clientId;

        ContractClient.ContractCreateRequest contractReq = new ContractClient.ContractCreateRequest();
        contractReq.setProjectId(c.getProjectId());
        contractReq.setFreelancerId(c.getFreelancerId());
        contractReq.setClientId(effectiveClientId);
        String terms = "Contract based on freelancer application.";
        if (c.getMessage() != null && !c.getMessage().isBlank()) {
            terms += "\n\nApplication message: " + c.getMessage();
        }
        contractReq.setTerms(terms);
        contractReq.setProposedBudget(c.getProposedBudget());
        contractReq.setApplicationMessage(c.getMessage());
        contractReq.setStatus("ACTIVE");
        Date startDate = new Date();
        contractReq.setStartDate(startDate);
        if (project.getDuration() != null) {
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.DAY_OF_MONTH, project.getDuration().intValue());
            contractReq.setEndDate(cal.getTime());
        }
        contractClient.createContract(contractReq);

        ProjectClient.ProjectUpdateRequest projectUpdate = new ProjectClient.ProjectUpdateRequest();
        projectUpdate.setTitle(project.getTitle());
        projectUpdate.setDescription(project.getDescription());
        projectUpdate.setMinBudget(project.getMinBudget());
        projectUpdate.setMaxBudget(project.getMaxBudget());
        projectUpdate.setDuration(project.getDuration());
        projectUpdate.setStatus("IN_PROGRESS");
        projectUpdate.setClientId(project.getClientId());
        projectClient.updateProjectStatus(c.getProjectId(), projectUpdate);

        return toResponseDTO(c);
    }

    @Transactional
    public CandidatureResponseDTO rejectCandidature(Long id) {
        Candidature c = candidatureRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Candidature not found with id: " + id));
        if (c.getStatus() != CandidatureStatus.PENDING) {
            throw new RuntimeException("Candidature is not pending");
        }
        c.setStatus(CandidatureStatus.REJECTED);
        return toResponseDTO(candidatureRepository.save(c));
    }

    @Transactional
    public void deleteCandidature(Long id) {
        if (!candidatureRepository.existsById(id)) {
            throw new RuntimeException("Candidature not found with id: " + id);
        }
        candidatureRepository.deleteById(id);
    }

    @Transactional
    public void payContract(Long contractId, Long clientId) {
        ContractClient.ContractResponse contract = contractClient.getContractById(contractId);
        if (contract == null) {
            throw new RuntimeException("Contract not found");
        }
        ProjectClient.ProjectResponse project = projectClient.getProjectById(contract.getProjectId());
        if (project == null || project.getClientId() == null || !project.getClientId().equals(clientId)) {
            throw new RuntimeException("Only the project owner can pay");
        }
        contractClient.payContract(contractId);
        ProjectClient.ProjectUpdateRequest update = new ProjectClient.ProjectUpdateRequest();
        update.setTitle(project.getTitle());
        update.setDescription(project.getDescription());
        update.setMinBudget(project.getMinBudget());
        update.setMaxBudget(project.getMaxBudget());
        update.setDuration(project.getDuration());
        update.setStatus("COMPLETED");
        update.setClientId(project.getClientId());
        projectClient.updateProjectStatus(contract.getProjectId(), update);
    }

    @Transactional
    public void cancelContract(Long contractId, Long clientId) {
        ContractClient.ContractResponse contract = contractClient.getContractById(contractId);
        if (contract == null) {
            throw new RuntimeException("Contract not found");
        }
        ProjectClient.ProjectResponse project = projectClient.getProjectById(contract.getProjectId());
        if (project == null || project.getClientId() == null || !project.getClientId().equals(clientId)) {
            throw new RuntimeException("Only the project owner can cancel");
        }
        contractClient.cancelContract(contractId);
        List<Candidature> accepted = candidatureRepository.findByProjectIdAndStatus(contract.getProjectId(), CandidatureStatus.ACCEPTED);
        for (Candidature c : accepted) {
            c.setStatus(CandidatureStatus.REJECTED);
            candidatureRepository.save(c);
        }
        List<Candidature> rejected = candidatureRepository.findByProjectIdAndStatus(contract.getProjectId(), CandidatureStatus.REJECTED);
        for (Candidature c : rejected) {
            c.setStatus(CandidatureStatus.PENDING);
            candidatureRepository.save(c);
        }
        ProjectClient.ProjectUpdateRequest update = new ProjectClient.ProjectUpdateRequest();
        update.setTitle(project.getTitle());
        update.setDescription(project.getDescription());
        update.setMinBudget(project.getMinBudget());
        update.setMaxBudget(project.getMaxBudget());
        update.setDuration(project.getDuration());
        update.setStatus("OPEN");
        update.setClientId(project.getClientId());
        projectClient.updateProjectStatus(contract.getProjectId(), update);
    }

    private CandidatureResponseDTO toResponseDTO(Candidature c) {
        CandidatureResponseDTO dto = new CandidatureResponseDTO();
        dto.setId(c.getId());
        dto.setProjectId(c.getProjectId());
        dto.setFreelancerId(c.getFreelancerId());
        dto.setMessage(c.getMessage());
        dto.setProposedBudget(c.getProposedBudget());
        dto.setStatus(c.getStatus());
        dto.setCreatedAt(c.getCreatedAt());
        return dto;
    }
}
