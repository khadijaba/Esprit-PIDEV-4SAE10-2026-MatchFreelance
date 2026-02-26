package com.freelancing.project.service;

import com.freelancing.project.client.ContractClient;
import com.freelancing.project.dto.ContractSummaryDTO;
import com.freelancing.project.dto.ProjectRequestDTO;
import com.freelancing.project.dto.ProjectResponseDTO;
import com.freelancing.project.entity.Project;
import com.freelancing.project.enums.ProjectStatus;
import com.freelancing.project.repository.ProjectRepository;
import com.freelancing.project.repository.ProjectSpecifications;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final ContractClient contractClient;

    @Transactional(readOnly = true)
    public List<ProjectResponseDTO> getAllProjects() {
        return projectRepository.findAll().stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<ProjectResponseDTO> getProjectsPage(String q, ProjectStatus status, Long clientId, Pageable pageable) {
        Specification<Project> spec = Specification.where(ProjectSpecifications.titleContains(q))
                .and(ProjectSpecifications.hasStatus(status))
                .and(ProjectSpecifications.hasClientId(clientId));

        return projectRepository.findAll(spec, pageable).map(this::toResponseDTO);
    }

    @Transactional(readOnly = true)
    public ProjectResponseDTO getProjectById(Long id) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Project not found with id: " + id));
        return toResponseDTO(project);
    }

    @Transactional(readOnly = true)
    public List<ProjectResponseDTO> getProjectsByStatus(ProjectStatus status) {
        return projectRepository.findByStatus(status).stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ProjectResponseDTO> searchProjectsByTitle(String title) {
        return projectRepository.findByTitleContainingIgnoreCase(title).stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ProjectResponseDTO> getProjectsByClientId(Long clientId) {
        return projectRepository.findByClientId(clientId).stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public ProjectResponseDTO createProject(ProjectRequestDTO requestDTO) {
        Project project = convertToEntity(requestDTO);
        Project saved = projectRepository.save(project);
        return toResponseDTO(saved);
    }

    @Transactional
    public ProjectResponseDTO updateProject(Long id, ProjectRequestDTO requestDTO) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Project not found with id: " + id));

        project.setTitle(requestDTO.getTitle());
        project.setDescription(requestDTO.getDescription());
        project.setMinBudget(requestDTO.getMinBudget());
        project.setMaxBudget(requestDTO.getMaxBudget());
        project.setDuration(requestDTO.getDuration());
        if (requestDTO.getStatus() != null) project.setStatus(requestDTO.getStatus());
        if (requestDTO.getClientId() != null) project.setClientId(requestDTO.getClientId());

        return toResponseDTO(projectRepository.save(project));
    }

    @Transactional
    public void deleteProject(Long id) {
        if (!projectRepository.existsById(id)) {
            throw new RuntimeException("Project not found with id: " + id);
        }
        projectRepository.deleteById(id);
    }

    private ProjectResponseDTO toResponseDTO(Project p) {
        ProjectResponseDTO dto = new ProjectResponseDTO();
        dto.setId(p.getId());
        dto.setTitle(p.getTitle());
        dto.setDescription(p.getDescription());
        dto.setMinBudget(p.getMinBudget());
        dto.setMaxBudget(p.getMaxBudget());
        dto.setDuration(p.getDuration());
        dto.setCreatedAt(p.getCreatedAt());
        dto.setStatus(p.getStatus());
        dto.setClientId(p.getClientId());
        if (p.getStatus() == ProjectStatus.IN_PROGRESS || p.getStatus() == ProjectStatus.COMPLETED) {
            List<ContractClient.ContractResponse> contracts = contractClient.getContractsByProjectId(p.getId());
            dto.setContracts(contracts != null ? contracts.stream().map(this::toContractSummary).collect(Collectors.toList()) : Collections.emptyList());
        } else {
            dto.setContracts(Collections.emptyList());
        }
        return dto;
    }

    private ContractSummaryDTO toContractSummary(ContractClient.ContractResponse c) {
        return new ContractSummaryDTO(
                c.getId(), c.getProjectId(), c.getFreelancerId(), c.getClientId(),
                c.getTerms(), c.getProposedBudget(), c.getApplicationMessage(), c.getStatus(),
                c.getStartDate(), c.getEndDate(), c.getCreatedAt()
        );
    }

    private Project convertToEntity(ProjectRequestDTO dto) {
        Project p = new Project();
        p.setTitle(dto.getTitle());
        p.setDescription(dto.getDescription());
        p.setMinBudget(dto.getMinBudget());
        p.setMaxBudget(dto.getMaxBudget());
        p.setDuration(dto.getDuration());
        p.setStatus(dto.getStatus() != null ? dto.getStatus() : ProjectStatus.OPEN);
        p.setClientId(dto.getClientId());
        return p;
    }
}
