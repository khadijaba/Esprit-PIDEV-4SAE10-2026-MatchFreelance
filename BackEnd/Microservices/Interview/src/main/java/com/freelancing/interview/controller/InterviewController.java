<<<<<<< HEAD
package com.freelancing.interview.controller;

import com.freelancing.interview.dto.InterviewRequestDTO;
import com.freelancing.interview.dto.InterviewResponseDTO;
import com.freelancing.interview.service.InterviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger log = LoggerFactory.getLogger(InterviewController.class);

    private final InterviewService interviewService;

    private static String safeMessage(Throwable e) {
        String m = e.getMessage();
        if (m != null && !m.isBlank()) {
            return m;
        }
        if (e.getCause() != null && e.getCause().getMessage() != null && !e.getCause().getMessage().isBlank()) {
            return e.getCause().getMessage();
        }
        return e.getClass().getSimpleName();
    }

    @GetMapping("/candidature/{candidatureId}/metrics")
    public ResponseEntity<?> getMetrics(
            @PathVariable Long candidatureId,
            @RequestParam Long requestingUserId) {
        try {
            return ResponseEntity.ok(interviewService.getMetrics(candidatureId, requestingUserId));
        } catch (RuntimeException e) {
            log.debug("getMetrics: {}", e.toString());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Collections.singletonMap("message", safeMessage(e)));
        } catch (Exception e) {
            log.error("getMetrics failed", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Collections.singletonMap("message", safeMessage(e)));
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
            log.debug("getInterviews: {}", e.toString());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Collections.singletonMap("message", safeMessage(e)));
        } catch (Exception e) {
            log.error("getInterviews failed", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Collections.singletonMap("message", safeMessage(e)));
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
            log.debug("scheduleInterview: {}", e.toString());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Collections.singletonMap("message", safeMessage(e)));
        } catch (Exception e) {
            log.error("scheduleInterview failed", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Collections.singletonMap("message", safeMessage(e)));
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
=======
package com.freelancing.interview.controller;

import com.freelancing.interview.dto.InterviewRequestDTO;
import com.freelancing.interview.dto.InterviewResponseDTO;
import com.freelancing.interview.service.InterviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger log = LoggerFactory.getLogger(InterviewController.class);

    private final InterviewService interviewService;

    private static String safeMessage(Throwable e) {
        String m = e.getMessage();
        if (m != null && !m.isBlank()) {
            return m;
        }
        if (e.getCause() != null && e.getCause().getMessage() != null && !e.getCause().getMessage().isBlank()) {
            return e.getCause().getMessage();
        }
        return e.getClass().getSimpleName();
    }

    @GetMapping("/candidature/{candidatureId}/metrics")
    public ResponseEntity<?> getMetrics(
            @PathVariable Long candidatureId,
            @RequestParam Long requestingUserId) {
        try {
            return ResponseEntity.ok(interviewService.getMetrics(candidatureId, requestingUserId));
        } catch (RuntimeException e) {
            log.debug("getMetrics: {}", e.toString());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Collections.singletonMap("message", safeMessage(e)));
        } catch (Exception e) {
            log.error("getMetrics failed", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Collections.singletonMap("message", safeMessage(e)));
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
            log.debug("getInterviews: {}", e.toString());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Collections.singletonMap("message", safeMessage(e)));
        } catch (Exception e) {
            log.error("getInterviews failed", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Collections.singletonMap("message", safeMessage(e)));
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
            log.debug("scheduleInterview: {}", e.toString());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Collections.singletonMap("message", safeMessage(e)));
        } catch (Exception e) {
            log.error("scheduleInterview failed", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Collections.singletonMap("message", safeMessage(e)));
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
>>>>>>> b7e93fa9abcd913d3ba37913b8481d5dd480ed43
