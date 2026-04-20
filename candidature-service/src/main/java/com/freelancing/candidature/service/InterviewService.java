package com.freelancing.candidature.service;

import com.freelancing.candidature.dto.InterviewRequestDTO;
import com.freelancing.candidature.dto.InterviewResponseDTO;
import com.freelancing.candidature.entity.Interview;
import com.freelancing.candidature.enums.InterviewStatus;
import com.freelancing.candidature.repository.CandidatureRepository;
import com.freelancing.candidature.repository.InterviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InterviewService {

    private final InterviewRepository interviewRepository;
    private final CandidatureRepository candidatureRepository;

    @Transactional(readOnly = true)
    public List<InterviewResponseDTO> getInterviewsByCandidatureId(Long candidatureId) {
        if (!candidatureRepository.existsById(candidatureId)) {
            throw new RuntimeException("Candidature not found with id: " + candidatureId);
        }
        return interviewRepository.findByCandidatureId(candidatureId).stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public InterviewResponseDTO scheduleInterview(Long candidatureId, InterviewRequestDTO requestDTO) {
        if (!candidatureRepository.existsById(candidatureId)) {
            throw new RuntimeException("Candidature not found with id: " + candidatureId);
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
    public InterviewResponseDTO updateInterview(Long candidatureId, Long interviewId, InterviewRequestDTO requestDTO) {
        Interview interview = interviewRepository.findById(interviewId)
                .orElseThrow(() -> new RuntimeException("Interview not found with id: " + interviewId));
        if (!interview.getCandidatureId().equals(candidatureId)) {
            throw new RuntimeException("Interview does not belong to candidature " + candidatureId);
        }
        if (requestDTO.getScheduledAt() != null) interview.setScheduledAt(requestDTO.getScheduledAt());
        if (requestDTO.getStatus() != null) interview.setStatus(requestDTO.getStatus());
        if (requestDTO.getNotes() != null) interview.setNotes(requestDTO.getNotes());
        return toResponseDTO(interviewRepository.save(interview));
    }

    @Transactional
    public void deleteInterview(Long candidatureId, Long interviewId) {
        Interview interview = interviewRepository.findById(interviewId)
                .orElseThrow(() -> new RuntimeException("Interview not found with id: " + interviewId));
        if (!interview.getCandidatureId().equals(candidatureId)) {
            throw new RuntimeException("Interview does not belong to candidature " + candidatureId);
        }
        interviewRepository.deleteById(interviewId);
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
