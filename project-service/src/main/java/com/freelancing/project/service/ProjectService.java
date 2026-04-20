package com.freelancing.project.service;

import com.freelancing.project.client.ContractClient;
import com.freelancing.project.client.UserClient;
import com.freelancing.project.dto.ContractSummaryDTO;
import com.freelancing.project.dto.ProjectRequestDTO;
import com.freelancing.project.dto.ProjectResponseDTO;
import com.freelancing.project.entity.Project;
import com.freelancing.project.enums.ProjectStatus;
import com.freelancing.project.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final ContractClient contractClient;
    private final UserClient userClient;

    @Transactional(readOnly = true)
    public List<ProjectResponseDTO> getAllProjects() {
        List<Project> projects = projectRepository.findAll();
        Map<Long, String> clientNames = resolveClientNames(projects);
        return projects.stream()
                .map(p -> toResponseDTO(p, clientNames))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ProjectResponseDTO getProjectById(Long id) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Project not found with id: " + id));
        return toResponseDTO(project, null);
    }

    @Transactional(readOnly = true)
    public List<ProjectResponseDTO> getProjectsByStatus(ProjectStatus status) {
        List<Project> projects = projectRepository.findByStatus(status);
        Map<Long, String> clientNames = resolveClientNames(projects);
        return projects.stream()
                .map(p -> toResponseDTO(p, clientNames))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ProjectResponseDTO> searchProjectsByTitle(String title) {
        List<Project> projects = projectRepository.findByTitleContainingIgnoreCase(title);
        Map<Long, String> clientNames = resolveClientNames(projects);
        return projects.stream()
                .map(p -> toResponseDTO(p, clientNames))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ProjectResponseDTO> getProjectsByClientId(Long clientId) {
        List<Project> projects = projectRepository.findByClientId(clientId);
        Map<Long, String> clientNames = resolveClientNames(projects);
        return projects.stream()
                .map(p -> toResponseDTO(p, clientNames))
                .collect(Collectors.toList());
    }

    @Transactional
    public ProjectResponseDTO createProject(ProjectRequestDTO requestDTO) {
        Project project = convertToEntity(requestDTO);
        Project saved = projectRepository.save(project);
        return toResponseDTO(saved, null);
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

        return toResponseDTO(projectRepository.save(project), null);
    }

    @Transactional
    public void deleteProject(Long id) {
        if (!projectRepository.existsById(id)) {
            throw new RuntimeException("Project not found with id: " + id);
        }
        projectRepository.deleteById(id);
    }

    private Map<Long, String> resolveClientNames(List<Project> projects) {
        List<Long> ids = projects.stream()
                .map(Project::getClientId)
                .filter(id -> id != null)
                .distinct()
                .toList();
        if (ids.isEmpty()) return Map.of();
        return userClient.getUsersByIds(ids).stream()
                .filter(u -> u != null && u.getId() != null && u.getName() != null)
                .collect(Collectors.toMap(UserClient.UserResponse::getId, UserClient.UserResponse::getName, (a, b) -> a));
    }

    private ProjectResponseDTO toResponseDTO(Project p, Map<Long, String> clientNames) {
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
        String clientName = null;
        if (p.getClientId() != null) {
            if (clientNames != null && clientNames.containsKey(p.getClientId())) {
                clientName = clientNames.get(p.getClientId());
            } else {
                UserClient.UserResponse user = userClient.getUserById(p.getClientId());
                clientName = user != null ? user.getName() : null;
            }
        }
        dto.setClientName(clientName);
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
                c.getFreelancerName(), c.getClientName(),
                c.getTerms(), c.getProposedBudget(), c.getExtraTasksBudget(), c.getApplicationMessage(), c.getStatus(),
                c.getStartDate(), c.getEndDate(), c.getCreatedAt(),
                c.getProgressPercent(),
                c.getPendingExtraAmount(), c.getPendingExtraReason(), c.getPendingExtraRequestedAt(),
                c.getClientRating(), c.getClientReview(), c.getClientReviewedAt()
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
