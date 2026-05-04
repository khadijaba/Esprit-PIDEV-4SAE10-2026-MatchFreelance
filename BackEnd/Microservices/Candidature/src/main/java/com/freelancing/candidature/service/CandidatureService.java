package com.freelancing.candidature.service;

import com.freelancing.candidature.client.ContractClient;
import com.freelancing.candidature.client.InterviewRemoteFeign;
import com.freelancing.candidature.client.ProjectClient;
import com.freelancing.candidature.client.UserClient;
import com.freelancing.candidature.dto.CandidatureRequestDTO;
import com.freelancing.candidature.dto.CandidatureResponseDTO;
import com.freelancing.candidature.dto.InterviewMetricsDto;
import com.freelancing.candidature.entity.Candidature;
import com.freelancing.candidature.enums.CandidatureStatus;
import com.freelancing.candidature.repository.CandidatureRepository;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CandidatureService {

    private final CandidatureRepository candidatureRepository;
    private final InterviewRemoteFeign interviewRemoteFeign;
    private final ProjectClient projectClient;
    private final ContractClient contractClient;
    private final UserClient userClient;
    private final AiMatchmakingService aiMatchmakingService;

    @Transactional(readOnly = true)
    public List<CandidatureResponseDTO> getAllCandidatures() {
        List<Candidature> candidatures = candidatureRepository.findAll();
        Map<Long, String> freelancerNames = resolveFreelancerNames(candidatures);
        return candidatures.stream()
                .map(c -> toResponseDTO(c, freelancerNames, null))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public CandidatureResponseDTO getCandidatureById(Long id) {
        Candidature c = candidatureRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Candidature not found with id: " + id));
        return toResponseDTO(c, null, null);
    }

    @Transactional(readOnly = true)
    public long countApplicationsForProject(Long projectId, Long clientId) {
        assertProjectOwner(projectId, clientId);
        return candidatureRepository.countByProjectId(projectId);
    }

    @Transactional(readOnly = true)
    public List<CandidatureResponseDTO> getCandidaturesByProjectId(Long projectId, Long clientId) {
        assertProjectOwner(projectId, clientId);
        List<Candidature> candidatures = candidatureRepository.findByProjectId(projectId);
        Map<Long, String> freelancerNames = resolveFreelancerNames(candidatures);
        // Ne pas appeler le microservice INTERVIEW ici : le front charge les entretiens via
        // GET /api/interviews/candidature/{id} (évite appels Feign en chaîne, timeouts et 500).
        return candidatures.stream()
                .map(c -> toResponseDTO(c, freelancerNames, null))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<CandidatureResponseDTO> getCandidaturesByFreelancerId(Long freelancerId) {
        List<Candidature> candidatures = candidatureRepository.findByFreelancerId(freelancerId);
        Map<Long, String> freelancerNames = resolveFreelancerNames(candidatures);
        return candidatures.stream()
                .map(c -> toResponseDTO(c, freelancerNames, freelancerId))
                .collect(Collectors.toList());
    }

    @Transactional
    public CandidatureResponseDTO createCandidature(CandidatureRequestDTO dto) {
        UserClient.UserResponse applicant = userClient.getUserById(dto.getFreelancerId());
        if (applicant == null) {
            Long fid = dto.getFreelancerId();
            throw new RuntimeException(
                    "Profil utilisateur introuvable pour freelancerId="
                            + fid
                            + ". Causes fréquentes : (1) cet id n’existe pas dans la base USER (ex. session navigateur "
                            + "avec un ancien userId après reset BDD — déconnectez-vous puis reconnectez-vous) ; "
                            + "(2) le microservice USER renvoie une erreur — testez GET /api/users/"
                            + fid
                            + " sur USER et consultez les logs Candidature (lignes WARN « USER: »). "
                            + "Eureka/JWT sont rarement en cause pour cet appel inter-service.");
        }
        String role = applicant.getRole();
        if (role == null || role.isBlank()) {
            throw new RuntimeException("Rôle utilisateur indisponible — resynchronisez le profil (connexion).");
        }
        if (!"FREELANCER".equalsIgnoreCase(role.trim())) {
            throw new RuntimeException("Seuls les comptes freelancer peuvent postuler à un projet.");
        }

        ProjectClient.ProjectResponse project = projectClient.getProjectById(dto.getProjectId());
        if (project == null) {
            throw new RuntimeException("Project not found with id: " + dto.getProjectId());
        }
        String projectStatus = project.getStatus() != null ? project.getStatus().trim().toUpperCase() : "";
        if (!"OPEN".equals(projectStatus)) {
            throw new RuntimeException("Project is not open for applications");
        }
        if (candidatureRepository.existsByProjectIdAndFreelancerId(dto.getProjectId(), dto.getFreelancerId())) {
            throw new RuntimeException("You have already applied to this project");
        }
        Double minB = project.getMinBudget();
        Double maxB = project.getMaxBudget();
        if (minB == null && maxB == null) {
            throw new RuntimeException("Budget du projet indisponible ou non défini — vérifiez le microservice Project.");
        }
        if (minB == null) {
            minB = maxB;
        }
        if (maxB == null) {
            maxB = minB;
        }
        if (dto.getProposedBudget() == null || dto.getProposedBudget() < minB || dto.getProposedBudget() > maxB) {
            throw new RuntimeException("Proposed budget must be between " + minB + " and " + maxB);
        }

        Candidature c = new Candidature();
        c.setProjectId(dto.getProjectId());
        c.setFreelancerId(dto.getFreelancerId());
        c.setMessage(dto.getMessage());
        c.setProposedBudget(dto.getProposedBudget());
        c.setExtraTasksBudget(dto.getExtraTasksBudget() != null && dto.getExtraTasksBudget() >= 0 ? dto.getExtraTasksBudget() : null);
        c.setStatus(CandidatureStatus.PENDING);
        
        aiMatchmakingService.analyzeCandidature(c, project);
        
        Candidature saved = candidatureRepository.save(c);
        return toResponseDTO(saved, null, null);
    }

    @Transactional
    public CandidatureResponseDTO updateCandidature(Long id, CandidatureRequestDTO dto) {
        Candidature c = candidatureRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Candidature not found with id: " + id));
        c.setMessage(dto.getMessage());
        return toResponseDTO(candidatureRepository.save(c), null, null);
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

        InterviewMetricsDto metrics;
        try {
            metrics = interviewRemoteFeign.getMetrics(c.getId(), clientId);
        } catch (FeignException e) {
            throw new RuntimeException(
                    "Service entretiens indisponible. Reessayez une fois le microservice INTERVIEW demarre.");
        }
        if (metrics == null || !Boolean.TRUE.equals(metrics.getEligibleForAcceptance())) {
            throw new RuntimeException(
                    "Marquez au moins un entretien comme termine (COMPLETED) avant d'accepter cette candidature.");
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
        contractReq.setExtraTasksBudget(c.getExtraTasksBudget());
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

        return toResponseDTO(c, null, clientId);
    }

    @Transactional
    public CandidatureResponseDTO rejectCandidature(Long id, Long clientId) {
        Candidature c = candidatureRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Candidature not found with id: " + id));
        ProjectClient.ProjectResponse project = projectClient.getProjectById(c.getProjectId());
        if (project == null) {
            throw new RuntimeException("Project not found");
        }
        if (project.getClientId() == null || !project.getClientId().equals(clientId)) {
            throw new RuntimeException("Only the project owner can reject candidatures");
        }
        if (c.getStatus() != CandidatureStatus.PENDING) {
            throw new RuntimeException("Candidature is not pending");
        }
        c.setStatus(CandidatureStatus.REJECTED);
        return toResponseDTO(candidatureRepository.save(c), null, clientId);
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

    private Map<Long, String> resolveFreelancerNames(List<Candidature> candidatures) {
        List<Long> ids = candidatures.stream()
                .map(Candidature::getFreelancerId)
                .filter(id -> id != null)
                .distinct()
                .toList();
        if (ids.isEmpty()) return Map.of();
        return userClient.getUsersByIds(ids).stream()
                .filter(u -> u != null && u.getId() != null && u.getName() != null)
                .collect(Collectors.toMap(UserClient.UserResponse::getId, UserClient.UserResponse::getName, (a, b) -> a));
    }

    private CandidatureResponseDTO toResponseDTO(Candidature c, Map<Long, String> freelancerNames, Long requestingUserIdForInterviews) {
        CandidatureResponseDTO dto = new CandidatureResponseDTO();
        dto.setId(c.getId());
        dto.setProjectId(c.getProjectId());
        dto.setFreelancerId(c.getFreelancerId());
        String freelancerName = freelancerNames != null && c.getFreelancerId() != null
                ? freelancerNames.get(c.getFreelancerId())
                : null;
        if (freelancerName == null && c.getFreelancerId() != null) {
            UserClient.UserResponse user = userClient.getUserById(c.getFreelancerId());
            freelancerName = user != null ? user.getName() : null;
        }
        dto.setFreelancerName(freelancerName);
        dto.setMessage(c.getMessage());
        dto.setProposedBudget(c.getProposedBudget());
        dto.setExtraTasksBudget(c.getExtraTasksBudget());
        dto.setStatus(c.getStatus());
        dto.setCreatedAt(c.getCreatedAt());
        dto.setAiMatchScore(c.getAiMatchScore());
        dto.setAiInsights(c.getAiInsights());
        if (requestingUserIdForInterviews == null) {
            dto.setInterviewCount(0);
            dto.setEligibleForAcceptance(false);
        } else {
            InterviewMetricsDto m = safeInterviewMetrics(c.getId(), requestingUserIdForInterviews);
            dto.setInterviewCount(m.getInterviewCount() != null ? m.getInterviewCount() : 0);
            dto.setEligibleForAcceptance(Boolean.TRUE.equals(m.getEligibleForAcceptance()));
        }
        return dto;
    }

    private InterviewMetricsDto safeInterviewMetrics(Long candidatureId, Long requestingUserId) {
        try {
            InterviewMetricsDto m = interviewRemoteFeign.getMetrics(candidatureId, requestingUserId);
            return m != null ? m : emptyInterviewMetrics();
        } catch (Exception e) {
            return emptyInterviewMetrics();
        }
    }

    private static InterviewMetricsDto emptyInterviewMetrics() {
        InterviewMetricsDto d = new InterviewMetricsDto();
        d.setInterviewCount(0);
        d.setEligibleForAcceptance(false);
        return d;
    }

    private void assertProjectOwner(Long projectId, Long clientId) {
        if (clientId == null) {
            throw new RuntimeException("clientId is required");
        }
        ProjectClient.ProjectResponse project = projectClient.getProjectById(projectId);
        if (project == null) {
            throw new RuntimeException("Project not found with id: " + projectId);
        }
        if (project.getClientId() == null || !project.getClientId().equals(clientId)) {
            throw new RuntimeException("Only the project owner can list candidatures for this project");
        }
    }
}
