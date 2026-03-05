package com.freelancing.candidature.controller;

import com.freelancing.candidature.dto.InterviewRequestDTO;
import com.freelancing.candidature.dto.InterviewResponseDTO;
import com.freelancing.candidature.service.InterviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/candidatures/{candidatureId}/interviews")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class InterviewController {

    private final InterviewService interviewService;

    @GetMapping
    public ResponseEntity<List<InterviewResponseDTO>> getInterviews(@PathVariable Long candidatureId) {
        try {
            return ResponseEntity.ok(interviewService.getInterviewsByCandidatureId(candidatureId));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping
    public ResponseEntity<InterviewResponseDTO> scheduleInterview(
            @PathVariable Long candidatureId,
            @Valid @RequestBody InterviewRequestDTO requestDTO) {
        try {
            InterviewResponseDTO created = interviewService.scheduleInterview(candidatureId, requestDTO);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/{interviewId}")
    public ResponseEntity<InterviewResponseDTO> updateInterview(
            @PathVariable Long candidatureId,
            @PathVariable Long interviewId,
            @Valid @RequestBody InterviewRequestDTO requestDTO) {
        try {
            return ResponseEntity.ok(interviewService.updateInterview(candidatureId, interviewId, requestDTO));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{interviewId}")
    public ResponseEntity<Void> deleteInterview(
            @PathVariable Long candidatureId,
            @PathVariable Long interviewId) {
        try {
            interviewService.deleteInterview(candidatureId, interviewId);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
