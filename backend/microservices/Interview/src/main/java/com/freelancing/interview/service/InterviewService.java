package com.freelancing.interview.service;

import com.freelancing.interview.client.CandidatureRemoteFeign;
import com.freelancing.interview.client.ProjectRemoteFeign;
import com.freelancing.interview.client.dto.CandidatureSnapshotDto;
import com.freelancing.interview.client.dto.ProjectRemotePayload;
import com.freelancing.interview.dto.InterviewMetricsDTO;
import com.freelancing.interview.dto.InterviewRequestDTO;
import com.freelancing.interview.dto.InterviewResponseDTO;
import com.freelancing.interview.entity.Interview;
import com.freelancing.interview.enums.InterviewStatus;
import com.freelancing.interview.repository.InterviewRepository;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InterviewService {

    /** Au moins un entretien doit etre marque termine (COMPLETED) avant acceptation / decision finale cote owner. */
    private static final Set<InterviewStatus> ELIGIBLE_FOR_ACCEPTANCE =
            EnumSet.of(InterviewStatus.COMPLETED);

    private final InterviewRepository interviewRepository;
    private final CandidatureRemoteFeign candidatureRemoteFeign;
    private final ProjectRemoteFeign projectRemoteFeign;

    @Transactional(readOnly = true)
    public InterviewMetricsDTO getMetrics(Long candidatureId, Long requestingUserId) {
        if (requestingUserId == null) {
            throw new RuntimeException("requestingUserId is required");
        }
        CandidatureSnapshotDto cand = loadCandidature(candidatureId);
        assertViewerAuthorized(cand, requestingUserId);
        long count = interviewRepository.countByCandidatureId(candidatureId);
        boolean eligible = cand.getStatus() != null
                && "PENDING".equalsIgnoreCase(cand.getStatus())
                && interviewRepository.existsByCandidatureIdAndStatusIn(candidatureId, ELIGIBLE_FOR_ACCEPTANCE);
        return new InterviewMetricsDTO((int) Math.min(count, Integer.MAX_VALUE), eligible);
    }

    @Transactional(readOnly = true)
    public List<InterviewResponseDTO> getInterviewsByCandidatureId(Long candidatureId, Long clientId) {
        assertProjectOwnerForCandidature(candidatureId, clientId);
        return interviewRepository.findByCandidatureId(candidatureId).stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    /** Freelancer read-only: liste des entretiens de sa propre candidature. */
    @Transactional(readOnly = true)
    public List<InterviewResponseDTO> getInterviewsForFreelancer(Long candidatureId, Long freelancerId) {
        if (freelancerId == null) {
            throw new RuntimeException("freelancerId is required");
        }
        CandidatureSnapshotDto cand = loadCandidature(candidatureId);
        if (cand.getFreelancerId() == null || !cand.getFreelancerId().equals(freelancerId)) {
            throw new RuntimeException("Not authorized to view interviews for this candidature");
        }
        return interviewRepository.findByCandidatureId(candidatureId).stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public InterviewResponseDTO scheduleInterview(Long candidatureId, Long clientId, InterviewRequestDTO requestDTO) {
        assertProjectOwnerForCandidature(candidatureId, clientId);
        CandidatureSnapshotDto cand = loadCandidature(candidatureId);
        if (cand.getStatus() == null || !"PENDING".equalsIgnoreCase(cand.getStatus())) {
            throw new RuntimeException("Can only schedule interviews for pending candidatures");
        }
        Interview interview = new Interview();
        interview.setCandidatureId(candidatureId);
        interview.setScheduledAt(requestDTO.getScheduledAt());
        interview.setStatus(requestDTO.getStatus() != null ? requestDTO.getStatus() : InterviewStatus.SCHEDULED);
        interview.setNotes(requestDTO.getNotes());
        Interview saved = interviewRepository.save(interview);
        return toResponseDTO(saved);
    }

    @Transactional
    public InterviewResponseDTO updateInterview(Long candidatureId, Long clientId, Long interviewId, InterviewRequestDTO requestDTO) {
        assertProjectOwnerForCandidature(candidatureId, clientId);
        Interview interview = interviewRepository.findById(interviewId)
                .orElseThrow(() -> new RuntimeException("Interview not found with id: " + interviewId));
        if (!interview.getCandidatureId().equals(candidatureId)) {
            throw new RuntimeException("Interview does not belong to candidature " + candidatureId);
        }
        if (requestDTO.getScheduledAt() != null) {
            interview.setScheduledAt(requestDTO.getScheduledAt());
        }
        if (requestDTO.getStatus() != null) {
            interview.setStatus(requestDTO.getStatus());
        }
        if (requestDTO.getNotes() != null) {
            interview.setNotes(requestDTO.getNotes());
        }
        return toResponseDTO(interviewRepository.save(interview));
    }

    @Transactional
    public void deleteInterview(Long candidatureId, Long clientId, Long interviewId) {
        assertProjectOwnerForCandidature(candidatureId, clientId);
        Interview interview = interviewRepository.findById(interviewId)
                .orElseThrow(() -> new RuntimeException("Interview not found with id: " + interviewId));
        if (!interview.getCandidatureId().equals(candidatureId)) {
            throw new RuntimeException("Interview does not belong to candidature " + candidatureId);
        }
        interviewRepository.deleteById(interviewId);
    }

    private CandidatureSnapshotDto loadCandidature(Long candidatureId) {
        try {
            CandidatureSnapshotDto cand = candidatureRemoteFeign.getById(candidatureId);
            if (cand == null || cand.getId() == null) {
                throw new RuntimeException("Candidature not found with id: " + candidatureId);
            }
            return cand;
        } catch (FeignException e) {
            int st = e.status();
            if (st == 404) {
                throw new RuntimeException("Candidature not found with id: " + candidatureId);
            }
            throw new RuntimeException(
                    "Service Candidature indisponible ou erreur (" + st + "). Vérifiez Eureka et le microservice CANDIDATURE.");
        }
    }

    private void assertViewerAuthorized(CandidatureSnapshotDto cand, Long requestingUserId) {
        ProjectRemotePayload project = loadProject(cand.getProjectId());
        if (project == null) {
            throw new RuntimeException("Project not found");
        }
        boolean owner = project.getProjectOwnerId() != null && project.getProjectOwnerId().equals(requestingUserId);
        boolean freelancer = cand.getFreelancerId() != null && cand.getFreelancerId().equals(requestingUserId);
        if (!owner && !freelancer) {
            throw new RuntimeException("Not authorized to view interview metrics for this candidature");
        }
    }

    private void assertProjectOwnerForCandidature(Long candidatureId, Long clientId) {
        if (clientId == null) {
            throw new RuntimeException("clientId is required");
        }
        CandidatureSnapshotDto cand = loadCandidature(candidatureId);
        ProjectRemotePayload project = loadProject(cand.getProjectId());
        if (project == null) {
            throw new RuntimeException("Project not found");
        }
        if (project.getProjectOwnerId() == null || !project.getProjectOwnerId().equals(clientId)) {
            throw new RuntimeException("Only the project owner can manage interviews for this candidature");
        }
    }

    private ProjectRemotePayload loadProject(Long projectId) {
        if (projectId == null) {
            return null;
        }
        try {
            return projectRemoteFeign.getById(projectId);
        } catch (FeignException e) {
            int st = e.status();
            if (st == 404) {
                return null;
            }
            throw new RuntimeException(
                    "Service Project indisponible ou erreur (" + st + "). Vérifiez Eureka et le microservice PROJECT.");
        }
    }

    private InterviewResponseDTO toResponseDTO(Interview i) {
        InterviewResponseDTO dto = new InterviewResponseDTO();
        dto.setId(i.getId());
        dto.setCandidatureId(i.getCandidatureId());
        dto.setScheduledAt(i.getScheduledAt());
        dto.setStatus(i.getStatus());
        dto.setNotes(i.getNotes());
        return dto;
    }
}
