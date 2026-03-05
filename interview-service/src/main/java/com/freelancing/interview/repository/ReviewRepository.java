package com.freelancing.interview.repository;

import com.freelancing.interview.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    Page<Review> findByInterviewId(Long interviewId, Pageable pageable);

    Page<Review> findByRevieweeIdOrderByCreatedAtDesc(Long revieweeId, Pageable pageable);

    Optional<Review> findByInterviewIdAndReviewerId(Long interviewId, Long reviewerId);

    boolean existsByInterviewIdAndReviewerId(Long interviewId, Long reviewerId);

    @Query("SELECT AVG(r.score) FROM Review r WHERE r.revieweeId = :revieweeId")
    Double averageScoreByRevieweeId(Long revieweeId);

    @Query("SELECT COUNT(r) FROM Review r WHERE r.revieweeId = :revieweeId")
    long countByRevieweeId(Long revieweeId);

    @Query("SELECT r.revieweeId AS revieweeId, AVG(r.score) AS avgScore, COUNT(r) AS cnt FROM Review r WHERE r.revieweeId IN :revieweeIds GROUP BY r.revieweeId")
    List<Object[]> findAverageScoreAndCountByRevieweeIdIn(@Param("revieweeIds") List<Long> revieweeIds);
}
