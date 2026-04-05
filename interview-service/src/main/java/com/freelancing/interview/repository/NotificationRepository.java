package com.freelancing.interview.repository;

import com.freelancing.interview.entity.Notification;
import com.freelancing.interview.enums.NotificationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    Page<Notification> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    boolean existsByInterviewIdAndType(Long interviewId, NotificationType type);

    boolean existsByUserIdAndInterviewIdAndType(Long userId, Long interviewId, NotificationType type);

    Optional<Notification> findByInterviewIdAndType(Long interviewId, NotificationType type);
}
