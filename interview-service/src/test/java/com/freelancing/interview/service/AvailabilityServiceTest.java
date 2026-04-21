package com.freelancing.interview.service;

import com.freelancing.interview.dto.AvailabilitySlotCreateRequestDTO;
import com.freelancing.interview.dto.AvailabilitySlotResponseDTO;
import com.freelancing.interview.entity.AvailabilitySlot;
import com.freelancing.interview.repository.AvailabilitySlotRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AvailabilityServiceTest {

    @Mock
    private AvailabilitySlotRepository slotRepository;

    @InjectMocks
    private AvailabilityService availabilityService;

    @Test
    void createSlot_shouldPersistUnbookedSlot() {
        AvailabilitySlotCreateRequestDTO request = new AvailabilitySlotCreateRequestDTO(
                Instant.parse("2026-04-17T09:00:00Z"),
                Instant.parse("2026-04-17T10:00:00Z")
        );

        when(slotRepository.save(any(AvailabilitySlot.class))).thenAnswer(invocation -> {
            AvailabilitySlot slot = invocation.getArgument(0);
            slot.setId(70L);
            return slot;
        });

        AvailabilitySlotResponseDTO result = availabilityService.createSlot(15L, request);

        assertEquals(70L, result.getId());
        assertEquals(15L, result.getFreelancerId());
        assertFalse(result.isBooked());
        verify(slotRepository).save(any(AvailabilitySlot.class));
    }

    @Test
    void createSlot_shouldRejectInvalidTimeRange() {
        AvailabilitySlotCreateRequestDTO request = new AvailabilitySlotCreateRequestDTO(
                Instant.parse("2026-04-17T11:00:00Z"),
                Instant.parse("2026-04-17T10:00:00Z")
        );

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> availabilityService.createSlot(15L, request));

        assertEquals("endAt must be after startAt", ex.getMessage());
    }

    @Test
    void deleteSlot_shouldBlockDeletionWhenBooked() {
        AvailabilitySlot slot = new AvailabilitySlot();
        slot.setId(88L);
        slot.setBooked(true);

        when(slotRepository.findById(88L)).thenReturn(Optional.of(slot));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> availabilityService.deleteSlot(88L));

        assertEquals("Cannot delete a booked slot", ex.getMessage());
    }
}

