package com.freelancing.interview.service;

import com.freelancing.interview.dto.ReviewCreateRequestDTO;
import com.freelancing.interview.dto.ReviewResponseDTO;
import com.freelancing.interview.entity.Interview;
import com.freelancing.interview.entity.Review;
import com.freelancing.interview.enums.InterviewStatus;
import com.freelancing.interview.repository.InterviewRepository;
import com.freelancing.interview.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final InterviewRepository interviewRepository;

    @Transactional
    public ReviewResponseDTO create(Long interviewId, Long reviewerId, ReviewCreateRequestDTO req) {
        Interview interview = interviewRepository.findById(interviewId)
                .orElseThrow(() -> new IllegalArgumentException("Interview not found: " + interviewId));
        if (interview.getStatus() != InterviewStatus.COMPLETED) {
            throw new IllegalStateException("Reviews can only be submitted for completed interviews");
        }
        if (!reviewerId.equals(interview.getOwnerId()) && !reviewerId.equals(interview.getFreelancerId())) {
            throw new IllegalArgumentException("Reviewer must be the interview owner or freelancer");
        }
        if (!req.getRevieweeId().equals(interview.getOwnerId()) && !req.getRevieweeId().equals(interview.getFreelancerId())) {
            throw new IllegalArgumentException("Reviewee must be the interview owner or freelancer");
        }
        if (req.getRevieweeId().equals(reviewerId)) {
            throw new IllegalArgumentException("Reviewer cannot review themselves");
        }
        if (reviewRepository.existsByInterviewIdAndReviewerId(interviewId, reviewerId)) {
            throw new IllegalStateException("You have already submitted a review for this interview");
        }

        Review review = new Review();
        review.setInterviewId(interviewId);
        review.setReviewerId(reviewerId);
        review.setRevieweeId(req.getRevieweeId());
        review.setScore(req.getScore());
        review.setComment(req.getComment() != null ? req.getComment().trim() : null);
        return toDto(reviewRepository.save(review));
    }

    @Transactional(readOnly = true)
    public Page<ReviewResponseDTO> getByInterviewId(Long interviewId, Pageable pageable) {
        return reviewRepository.findByInterviewId(interviewId, pageable).map(this::toDto);
    }

    @Transactional(readOnly = true)
    public Page<ReviewResponseDTO> getByRevieweeId(Long revieweeId, Pageable pageable) {
        return reviewRepository.findByRevieweeIdOrderByCreatedAtDesc(revieweeId, pageable).map(this::toDto);
    }

    @Transactional(readOnly = true)
    public ReviewAggregateDTO getAggregateByRevieweeId(Long revieweeId) {
        Double avg = reviewRepository.averageScoreByRevieweeId(revieweeId);
        long count = reviewRepository.countByRevieweeId(revieweeId);
        return new ReviewAggregateDTO(revieweeId, avg != null ? Math.round(avg * 100) / 100.0 : null, count);
    }

    private ReviewResponseDTO toDto(Review r) {
        return new ReviewResponseDTO(
                r.getId(),
                r.getInterviewId(),
                r.getReviewerId(),
                r.getRevieweeId(),
                r.getScore(),
                r.getComment(),
                r.getCreatedAt()
        );
    }

    public record ReviewAggregateDTO(Long revieweeId, Double averageScore, long reviewCount) {}
}
