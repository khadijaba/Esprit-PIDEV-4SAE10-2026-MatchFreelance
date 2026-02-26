package com.freelancing.interview.service;

import com.freelancing.interview.dto.AvailabilitySlotBatchCreateRequestDTO;
import com.freelancing.interview.dto.AvailabilitySlotCreateRequestDTO;
import com.freelancing.interview.dto.AvailabilitySlotResponseDTO;
import com.freelancing.interview.entity.AvailabilitySlot;
import com.freelancing.interview.repository.AvailabilitySlotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AvailabilityService {

    private final AvailabilitySlotRepository slotRepository;

    @Transactional
    public AvailabilitySlotResponseDTO createSlot(Long freelancerId, AvailabilitySlotCreateRequestDTO req) {
        if (req.getStartAt() == null || req.getEndAt() == null) {
            throw new IllegalArgumentException("startAt and endAt are required");
        }
        if (!req.getEndAt().isAfter(req.getStartAt())) {
            throw new IllegalArgumentException("endAt must be after startAt");
        }

        AvailabilitySlot slot = new AvailabilitySlot();
        slot.setFreelancerId(freelancerId);
        slot.setStartAt(req.getStartAt());
        slot.setEndAt(req.getEndAt());
        slot.setBooked(false);
        slot.setBookedInterviewId(null);

        return toDto(slotRepository.save(slot));
    }

    @Transactional
    public List<AvailabilitySlotResponseDTO> createSlotsBatch(Long freelancerId, AvailabilitySlotBatchCreateRequestDTO batch) {
        if (batch.getSlots() == null || batch.getSlots().isEmpty()) {
            throw new IllegalArgumentException("At least one slot is required");
        }
        return batch.getSlots().stream()
                .map(req -> createSlot(freelancerId, req))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<AvailabilitySlotResponseDTO> listSlots(
            Long freelancerId,
            Instant from,
            Instant to,
            boolean onlyFree,
            Pageable pageable
    ) {
        Page<AvailabilitySlot> page;

        boolean hasRange = from != null && to != null;
        if (hasRange && onlyFree) {
            page = slotRepository.findByFreelancerIdAndBookedFalseAndStartAtGreaterThanEqualAndEndAtLessThanEqual(
                    freelancerId, from, to, pageable
            );
        } else if (hasRange) {
            page = slotRepository.findByFreelancerIdAndStartAtGreaterThanEqualAndEndAtLessThanEqual(
                    freelancerId, from, to, pageable
            );
        } else if (onlyFree) {
            page = slotRepository.findByFreelancerIdAndBookedFalse(freelancerId, pageable);
        } else {
            page = slotRepository.findByFreelancerId(freelancerId, pageable);
        }

        return page.map(this::toDto);
    }

    @Transactional
    public void deleteSlot(Long slotId) {
        AvailabilitySlot slot = slotRepository.findById(slotId)
                .orElseThrow(() -> new RuntimeException("Availability slot not found with id: " + slotId));
        if (slot.isBooked()) {
            throw new IllegalStateException("Cannot delete a booked slot");
        }
        slotRepository.deleteById(slotId);
    }

    private AvailabilitySlotResponseDTO toDto(AvailabilitySlot slot) {
        return new AvailabilitySlotResponseDTO(
                slot.getId(),
                slot.getFreelancerId(),
                slot.getStartAt(),
                slot.getEndAt(),
                slot.isBooked(),
                slot.getBookedInterviewId()
        );
    }
}

