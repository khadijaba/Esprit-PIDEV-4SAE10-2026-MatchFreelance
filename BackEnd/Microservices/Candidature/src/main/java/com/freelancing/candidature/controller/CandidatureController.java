package com.freelancing.candidature.controller;

import com.freelancing.candidature.dto.BudgetStatsDTO;
import com.freelancing.candidature.dto.CandidatureRequestDTO;
import com.freelancing.candidature.dto.CandidatureResponseDTO;
import com.freelancing.candidature.dto.RankedCandidatureDTO;
import com.freelancing.candidature.service.CandidatureRankingService;
import com.freelancing.candidature.service.CandidatureService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/api/candidatures")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class CandidatureController {

    private final CandidatureService candidatureService;
    private final CandidatureRankingService candidatureRankingService;

    private static String safeErrorMessage(Throwable e) {
        String m = e.getMessage();
        if ((m == null || m.isBlank()) && e.getCause() != null) {
            m = e.getCause().getMessage();
        }
        return (m != null && !m.isBlank()) ? m : "Requête invalide";
    }

    @GetMapping
    public ResponseEntity<List<CandidatureResponseDTO>> getAllCandidatures() {
        return ResponseEntity.ok(candidatureService.getAllCandidatures());
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
    public ResponseEntity<?> getCandidaturesByProject(
            @PathVariable Long projectId,
            @RequestParam Long clientId) {
        try {
            return ResponseEntity.ok(candidatureService.getCandidaturesByProjectId(projectId, clientId));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Collections.singletonMap("message", safeErrorMessage(e)));
        }
    }

    @GetMapping("/project/{projectId}/ranked")
    public ResponseEntity<?> getRankedCandidatures(
            @PathVariable Long projectId,
            @RequestParam Long clientId,
            @RequestParam(required = false) Double minScore,
            @RequestParam(required = false) Integer limit) {
        try {
            return ResponseEntity.ok(
                    candidatureRankingService.getRankedCandidatures(projectId, clientId, minScore, limit));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Collections.singletonMap("message", safeErrorMessage(e)));
        }
    }

    @GetMapping("/project/{projectId}/budget-stats")
    public ResponseEntity<?> getBudgetStats(
            @PathVariable Long projectId,
            @RequestParam Long clientId) {
        try {
            return ResponseEntity.ok(candidatureRankingService.getBudgetStats(projectId, clientId));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Collections.singletonMap("message", safeErrorMessage(e)));
        }
    }

    @GetMapping("/freelancer/{freelancerId}")
    public ResponseEntity<List<CandidatureResponseDTO>> getCandidaturesByFreelancer(@PathVariable Long freelancerId) {
        return ResponseEntity.ok(candidatureService.getCandidaturesByFreelancerId(freelancerId));
    }

    @PostMapping
    public ResponseEntity<?> createCandidature(@Valid @RequestBody CandidatureRequestDTO request) {
        try {
            CandidatureResponseDTO created = candidatureService.createCandidature(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Collections.singletonMap("message", safeErrorMessage(e)));
        }
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
    public ResponseEntity<?> acceptCandidature(
            @PathVariable Long id,
            @RequestParam Long clientId) {
        try {
            return ResponseEntity.ok(candidatureService.acceptCandidature(id, clientId));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Collections.singletonMap("message", safeErrorMessage(e)));
        }
    }

    @PutMapping("/{id:\\d+}/reject")
    public ResponseEntity<?> rejectCandidature(
            @PathVariable Long id,
            @RequestParam Long clientId) {
        try {
            return ResponseEntity.ok(candidatureService.rejectCandidature(id, clientId));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Collections.singletonMap("message", safeErrorMessage(e)));
        }
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
    public ResponseEntity<?> payContract(
            @PathVariable Long contractId,
            @RequestParam Long clientId) {
        try {
            candidatureService.payContract(contractId, clientId);
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Collections.singletonMap("message", safeErrorMessage(e)));
        }
    }

    @PutMapping("/contract/{contractId}/cancel")
    public ResponseEntity<?> cancelContract(
            @PathVariable Long contractId,
            @RequestParam Long clientId) {
        try {
            candidatureService.cancelContract(contractId, clientId);
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Collections.singletonMap("message", safeErrorMessage(e)));
        }
    }
}
