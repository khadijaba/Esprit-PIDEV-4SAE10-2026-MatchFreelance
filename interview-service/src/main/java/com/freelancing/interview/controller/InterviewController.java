package com.freelancing.interview.controller;

import com.freelancing.interview.dto.InterviewCreateRequestDTO;
import com.freelancing.interview.dto.InterviewResponseDTO;
import com.freelancing.interview.dto.InterviewUpdateRequestDTO;
import com.freelancing.interview.dto.VisioRoomResponseDTO;
import com.freelancing.interview.enums.InterviewStatus;
import com.freelancing.interview.enums.MeetingMode;
import com.freelancing.interview.service.InterviewService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.validation.annotation.Validated;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

@RestController
@RequestMapping("/api/interviews")
@RequiredArgsConstructor
@Validated
@CrossOrigin(origins = "*")
public class InterviewController {

    private final InterviewService interviewService;

    @PostMapping
    public ResponseEntity<InterviewResponseDTO> create(@Valid @RequestBody InterviewCreateRequestDTO req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(interviewService.create(req));
    }

    @GetMapping
    public ResponseEntity<Page<InterviewResponseDTO>> search(
            @RequestParam(required = false) Long freelancerId,
            @RequestParam(required = false) Long ownerId,
            @RequestParam(required = false) Long projectId,
            @RequestParam(required = false) Long candidatureId,
            @RequestParam(required = false) InterviewStatus status,
            @RequestParam(required = false) MeetingMode mode,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            @PageableDefault(size = 10) Pageable pageable
    ) {
        return ResponseEntity.ok(interviewService.search(
                freelancerId, ownerId, projectId, candidatureId, status, mode, from, to, pageable
        ));
    }

    @PutMapping("/{interviewId}")
    public ResponseEntity<InterviewResponseDTO> update(
            @PathVariable @Min(1) Long interviewId,
            @Valid @RequestBody InterviewUpdateRequestDTO req
    ) {
        return ResponseEntity.ok(interviewService.update(interviewId, req));
    }

    @PostMapping("/{interviewId}/confirm")
    public ResponseEntity<InterviewResponseDTO> confirm(@PathVariable @Min(1) Long interviewId) {
        return ResponseEntity.ok(interviewService.confirm(interviewId));
    }

    @PostMapping("/{interviewId}/cancel")
    public ResponseEntity<InterviewResponseDTO> cancel(@PathVariable @Min(1) Long interviewId) {
        return ResponseEntity.ok(interviewService.cancel(interviewId));
    }

    @PostMapping("/{interviewId}/no-show")
    public ResponseEntity<InterviewResponseDTO> noShow(@PathVariable @Min(1) Long interviewId) {
        return ResponseEntity.ok(interviewService.noShow(interviewId));
    }

    @PostMapping("/{interviewId}/complete")
    public ResponseEntity<InterviewResponseDTO> complete(@PathVariable @Min(1) Long interviewId) {
        return ResponseEntity.ok(interviewService.complete(interviewId));
    }

    @GetMapping("/{interviewId}/visio-room")
    public ResponseEntity<VisioRoomResponseDTO> getOrCreateVisioRoom(@PathVariable @Min(1) Long interviewId) {
        return ResponseEntity.ok(interviewService.getOrCreateVisioRoom(interviewId));
    }

    @GetMapping("/{interviewId}/ics")
    public ResponseEntity<String> downloadIcs(@PathVariable @Min(1) Long interviewId) {
        String ics = interviewService.generateIcs(interviewId);
        return ResponseEntity.ok()
                .header("Content-Type", "text/calendar; charset=utf-8")
                .header("Content-Disposition", "attachment; filename=interview-" + interviewId + ".ics")
                .body(ics);
    }

    @DeleteMapping("/{interviewId}")
    public ResponseEntity<Void> delete(@PathVariable @Min(1) Long interviewId) {
        interviewService.delete(interviewId);
        return ResponseEntity.noContent().build();
    }
}

