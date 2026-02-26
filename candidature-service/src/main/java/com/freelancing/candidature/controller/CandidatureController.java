package com.freelancing.candidature.controller;

import com.freelancing.candidature.dto.CandidatureRequestDTO;
import com.freelancing.candidature.dto.CandidatureResponseDTO;
import com.freelancing.candidature.enums.CandidatureStatus;
import com.freelancing.candidature.service.CandidatureService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/candidatures")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class CandidatureController {

    private final CandidatureService candidatureService;

    @GetMapping
    public ResponseEntity<List<CandidatureResponseDTO>> getAllCandidatures() {
        return ResponseEntity.ok(candidatureService.getAllCandidatures());
    }

    @GetMapping("/page")
    public ResponseEntity<Page<CandidatureResponseDTO>> getCandidaturesPage(
            @RequestParam(required = false) Long projectId,
            @RequestParam(required = false) Long freelancerId,
            @RequestParam(required = false) CandidatureStatus status,
            @PageableDefault(size = 10) Pageable pageable
    ) {
        return ResponseEntity.ok(candidatureService.getCandidaturesPage(projectId, freelancerId, status, pageable));
    }

    @GetMapping("/{id:\\d+}")
    public ResponseEntity<CandidatureResponseDTO> getCandidatureById(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(candidatureService.getCandidatureById(id));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/project/{projectId}")
    public ResponseEntity<List<CandidatureResponseDTO>> getCandidaturesByProject(@PathVariable Long projectId) {
        return ResponseEntity.ok(candidatureService.getCandidaturesByProjectId(projectId));
    }

    @GetMapping("/freelancer/{freelancerId}")
    public ResponseEntity<List<CandidatureResponseDTO>> getCandidaturesByFreelancer(@PathVariable Long freelancerId) {
        return ResponseEntity.ok(candidatureService.getCandidaturesByFreelancerId(freelancerId));
    }

    @PostMapping
    public ResponseEntity<CandidatureResponseDTO> createCandidature(@Valid @RequestBody CandidatureRequestDTO request) {
        CandidatureResponseDTO created = candidatureService.createCandidature(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id:\\d+}")
    public ResponseEntity<CandidatureResponseDTO> updateCandidature(
            @PathVariable Long id,
            @Valid @RequestBody CandidatureRequestDTO request) {
        try {
            return ResponseEntity.ok(candidatureService.updateCandidature(id, request));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/{id:\\d+}/accept")
    public ResponseEntity<CandidatureResponseDTO> acceptCandidature(
            @PathVariable Long id,
            @RequestParam(defaultValue = "1") Long clientId) {
        return ResponseEntity.ok(candidatureService.acceptCandidature(id, clientId));
    }

    @PutMapping("/{id:\\d+}/reject")
    public ResponseEntity<CandidatureResponseDTO> rejectCandidature(@PathVariable Long id) {
        return ResponseEntity.ok(candidatureService.rejectCandidature(id));
    }

    @DeleteMapping("/{id:\\d+}")
    public ResponseEntity<Void> deleteCandidature(@PathVariable Long id) {
        try {
            candidatureService.deleteCandidature(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/contract/{contractId}/pay")
    public ResponseEntity<Void> payContract(
            @PathVariable Long contractId,
            @RequestParam(defaultValue = "1") Long clientId) {
        candidatureService.payContract(contractId, clientId);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/contract/{contractId}/cancel")
    public ResponseEntity<Void> cancelContract(
            @PathVariable Long contractId,
            @RequestParam(defaultValue = "1") Long clientId) {
        candidatureService.cancelContract(contractId, clientId);
        return ResponseEntity.ok().build();
    }
}
