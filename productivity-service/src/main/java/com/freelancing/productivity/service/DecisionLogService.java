package com.freelancing.productivity.service;

import com.freelancing.productivity.dto.DecisionLogCreateRequestDTO;
import com.freelancing.productivity.dto.DecisionLogResponseDTO;
import com.freelancing.productivity.entity.DecisionLogEntry;
import com.freelancing.productivity.repository.DecisionLogEntryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DecisionLogService {

    private final DecisionLogEntryRepository decisionLogEntryRepository;

    @Transactional
    public DecisionLogResponseDTO create(Long ownerId, DecisionLogCreateRequestDTO request) {
        DecisionLogEntry entry = new DecisionLogEntry();
        entry.setOwnerId(ownerId);
        entry.setTaskId(request.getTaskId());
        entry.setDecisionType(request.getDecisionType().trim());
        entry.setReason(request.getReason().trim());
        return toDto(decisionLogEntryRepository.save(entry));
    }

    @Transactional(readOnly = true)
    public List<DecisionLogResponseDTO> listByOwner(Long ownerId) {
        return decisionLogEntryRepository.findByOwnerIdOrderByCreatedAtDesc(ownerId)
                .stream().map(this::toDto).toList();
    }

    private DecisionLogResponseDTO toDto(DecisionLogEntry entry) {
        DecisionLogResponseDTO dto = new DecisionLogResponseDTO();
        dto.setId(entry.getId());
        dto.setOwnerId(entry.getOwnerId());
        dto.setTaskId(entry.getTaskId());
        dto.setDecisionType(entry.getDecisionType());
        dto.setReason(entry.getReason());
        dto.setCreatedAt(entry.getCreatedAt());
        return dto;
    }
}

