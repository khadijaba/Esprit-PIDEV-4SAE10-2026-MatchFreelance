package com.freelancing.interview.controller;

import com.freelancing.interview.dto.ReviewCreateRequestDTO;
import com.freelancing.interview.dto.ReviewResponseDTO;
import com.freelancing.interview.service.ReviewService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Validated
@CrossOrigin(origins = "*")
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping("/interviews/{interviewId}/reviews")
    public ResponseEntity<ReviewResponseDTO> create(
            @PathVariable @Min(1) Long interviewId,
            @RequestParam @Min(1) Long reviewerId,
            @Valid @RequestBody ReviewCreateRequestDTO req
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(reviewService.create(interviewId, reviewerId, req));
    }

    @GetMapping("/interviews/{interviewId}/reviews")
    public ResponseEntity<Page<ReviewResponseDTO>> getByInterview(
            @PathVariable @Min(1) Long interviewId,
            @PageableDefault(size = 10) Pageable pageable
    ) {
        return ResponseEntity.ok(reviewService.getByInterviewId(interviewId, pageable));
    }

    @GetMapping("/reviews/reviewee/{revieweeId}")
    public ResponseEntity<Page<ReviewResponseDTO>> getByReviewee(
            @PathVariable @Min(1) Long revieweeId,
            @PageableDefault(size = 10) Pageable pageable
    ) {
        return ResponseEntity.ok(reviewService.getByRevieweeId(revieweeId, pageable));
    }

    @GetMapping("/reviews/reviewee/{revieweeId}/aggregate")
    public ResponseEntity<ReviewService.ReviewAggregateDTO> getAggregate(@PathVariable @Min(1) Long revieweeId) {
        return ResponseEntity.ok(reviewService.getAggregateByRevieweeId(revieweeId));
    }
}
