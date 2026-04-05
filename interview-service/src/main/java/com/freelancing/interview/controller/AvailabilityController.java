package com.freelancing.interview.controller;

import com.freelancing.interview.dto.AvailabilitySlotBatchCreateRequestDTO;
import com.freelancing.interview.dto.AvailabilitySlotCreateRequestDTO;
import com.freelancing.interview.dto.AvailabilitySlotResponseDTO;
import com.freelancing.interview.service.AvailabilityService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.validation.annotation.Validated;

import java.util.List;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

@RestController
@RequestMapping("/api/availability")
@RequiredArgsConstructor
@Validated
@CrossOrigin(origins = "*")
public class AvailabilityController {

    private final AvailabilityService availabilityService;

    @PostMapping("/freelancers/{freelancerId}/slots")
    public ResponseEntity<AvailabilitySlotResponseDTO> createSlot(
            @PathVariable @Min(1) Long freelancerId,
            @Valid @RequestBody AvailabilitySlotCreateRequestDTO req
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(availabilityService.createSlot(freelancerId, req));
    }

    @PostMapping("/freelancers/{freelancerId}/slots/batch")
    public ResponseEntity<List<AvailabilitySlotResponseDTO>> createSlotsBatch(
            @PathVariable @Min(1) Long freelancerId,
            @Valid @RequestBody AvailabilitySlotBatchCreateRequestDTO batch
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(availabilityService.createSlotsBatch(freelancerId, batch));
    }

    @GetMapping("/freelancers/{freelancerId}/slots")
    public ResponseEntity<Page<AvailabilitySlotResponseDTO>> listSlots(
            @PathVariable @Min(1) Long freelancerId,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            @RequestParam(defaultValue = "true") boolean onlyFree,
            @PageableDefault(size = 10) Pageable pageable
    ) {
        return ResponseEntity.ok(availabilityService.listSlots(freelancerId, from, to, onlyFree, pageable));
    }

    @DeleteMapping("/slots/{slotId}")
    public ResponseEntity<Void> deleteSlot(@PathVariable @Min(1) Long slotId) {
        availabilityService.deleteSlot(slotId);
        return ResponseEntity.noContent().build();
    }
}

