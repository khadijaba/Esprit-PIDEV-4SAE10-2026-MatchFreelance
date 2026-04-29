package com.freelancing.interview.controller;

import com.freelancing.interview.dto.InterviewRequestDTO;
import com.freelancing.interview.dto.InterviewResponseDTO;
import com.freelancing.interview.service.InterviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/api/interviews")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class InterviewController {

    private final InterviewService interviewService;

    @GetMapping("/candidature/{candidatureId}/metrics")
    public ResponseEntity<?> getMetrics(
            @PathVariable Long candidatureId,
            @RequestParam Long requestingUserId) {
        try {
            return ResponseEntity.ok(interviewService.getMetrics(candidatureId, requestingUserId));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Collections.singletonMap("message", e.getMessage()));
        }
    }

    @GetMapping("/candidature/{candidatureId}")
    public ResponseEntity<?> getInterviews(
            @PathVariable Long candidatureId,
            @RequestParam Long clientId) {
        try {
            List<InterviewResponseDTO> list = interviewService.getInterviewsByCandidatureId(candidatureId, clientId);
            return ResponseEntity.ok(list);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Collections.singletonMap("message", e.getMessage()));
        }
    }

    @PostMapping("/candidature/{candidatureId}")
    public ResponseEntity<?> scheduleInterview(
            @PathVariable Long candidatureId,
            @RequestParam Long clientId,
            @Valid @RequestBody InterviewRequestDTO requestDTO) {
        try {
            InterviewResponseDTO created = interviewService.scheduleInterview(candidatureId, clientId, requestDTO);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Collections.singletonMap("message", e.getMessage()));
        }
    }

    @PutMapping("/candidature/{candidatureId}/{interviewId}")
    public ResponseEntity<?> updateInterview(
            @PathVariable Long candidatureId,
            @PathVariable Long interviewId,
            @RequestParam Long clientId,
            @Valid @RequestBody InterviewRequestDTO requestDTO) {
        try {
            return ResponseEntity.ok(
                    interviewService.updateInterview(candidatureId, clientId, interviewId, requestDTO));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Collections.singletonMap("message", e.getMessage()));
        }
    }

    @DeleteMapping("/candidature/{candidatureId}/{interviewId}")
    public ResponseEntity<?> deleteInterview(
            @PathVariable Long candidatureId,
            @PathVariable Long interviewId,
            @RequestParam Long clientId) {
        try {
            interviewService.deleteInterview(candidatureId, clientId, interviewId);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Collections.singletonMap("message", e.getMessage()));
        }
    }
}
