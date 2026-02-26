package com.freelancing.interview.service;

import com.freelancing.interview.dto.NotificationResponseDTO;
import com.freelancing.interview.entity.Interview;
import com.freelancing.interview.entity.Notification;
import com.freelancing.interview.enums.NotificationType;
import com.freelancing.interview.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter
            .ofPattern("dd/MM/yyyy HH:mm", Locale.FRANCE)
            .withZone(ZoneOffset.UTC);

    private final NotificationRepository notificationRepository;

    /** Emit a notification to one user (e.g. freelancer or owner). */
    @Transactional
    public void emit(Long userId, Long interviewId, NotificationType type, String message) {
        Notification n = new Notification();
        n.setUserId(userId);
        n.setInterviewId(interviewId);
        n.setType(type);
        n.setMessage(message != null ? message : defaultMessage(type, null));
        notificationRepository.save(n);
    }

    /** Emit the same notification to both freelancer and owner. */
    @Transactional
    public void emitToBoth(Interview interview, NotificationType type) {
        String msg = defaultMessage(type, interview);
        emit(interview.getFreelancerId(), interview.getId(), type, msg);
        emit(interview.getOwnerId(), interview.getId(), type, msg);
    }

    @Transactional(readOnly = true)
    public Page<NotificationResponseDTO> listByUser(Long userId, Pageable pageable) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(this::toDto);
    }

    @Transactional
    public NotificationResponseDTO markAsRead(Long notificationId, Long userId) {
        Notification n = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new IllegalArgumentException("Notification not found: " + notificationId));
        if (!n.getUserId().equals(userId)) {
            throw new IllegalArgumentException("Notification does not belong to user");
        }
        if (n.getReadAt() == null) {
            n.setReadAt(Instant.now());
            n = notificationRepository.save(n);
        }
        return toDto(n);
    }

    @Transactional
    public void markAllAsReadForUser(Long userId) {
        notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, Pageable.unpaged())
                .getContent()
                .stream()
                .filter(n -> n.getReadAt() == null)
                .forEach(n -> {
                    n.setReadAt(Instant.now());
                    notificationRepository.save(n);
                });
    }

    public boolean existsReminderForUser(Long userId, Long interviewId, NotificationType type) {
        return notificationRepository.existsByUserIdAndInterviewIdAndType(userId, interviewId, type);
    }

    @Transactional
    public void createReminderIfAbsent(Long userId, Long interviewId, NotificationType type, String message) {
        if (notificationRepository.existsByUserIdAndInterviewIdAndType(userId, interviewId, type)) return;
        emit(userId, interviewId, type, message);
    }

    private String defaultMessage(NotificationType type, Interview i) {
        String time = i != null ? TIME_FORMAT.format(i.getStartAt()) : "";
        return switch (type) {
            case INTERVIEW_PROPOSED -> "Un entretien vous a été proposé" + (time.isEmpty() ? "." : " pour le " + time + ".");
            case INTERVIEW_CONFIRMED -> "L'entretien du " + time + " a été confirmé.";
            case INTERVIEW_CANCELLED -> "L'entretien du " + time + " a été annulé.";
            case INTERVIEW_COMPLETED -> "L'entretien du " + time + " est marqué comme terminé.";
            case INTERVIEW_NO_SHOW -> "Absence non excusée enregistrée pour l'entretien du " + time + ".";
            case REMINDER_24H -> "Rappel : entretien dans 24h (".concat(time).concat(").");
            case REMINDER_1H -> "Rappel : entretien dans 1h (".concat(time).concat(").");
            default -> "Notification";
        };
    }

    private NotificationResponseDTO toDto(Notification n) {
        return new NotificationResponseDTO(
                n.getId(),
                n.getUserId(),
                n.getInterviewId(),
                n.getType(),
                n.getMessage(),
                n.getReadAt(),
                n.getCreatedAt()
        );
    }
}
