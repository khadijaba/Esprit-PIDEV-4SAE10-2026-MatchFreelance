package com.freelancing.interview.service;

import com.freelancing.interview.entity.Interview;
import com.freelancing.interview.enums.InterviewStatus;
import com.freelancing.interview.enums.NotificationType;
import com.freelancing.interview.repository.InterviewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/**
 * Creates in-app reminders (rappels) for upcoming interviews: 24h and 1h before start.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ReminderScheduler {

    private static final Duration WINDOW_24H = Duration.ofMinutes(30); // e.g. 23h30–24h30 before
    private static final Duration WINDOW_1H = Duration.ofMinutes(15);   // e.g. 55min–1h05 before
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter
            .ofPattern("dd/MM/yyyy HH:mm", Locale.FRANCE)
            .withZone(ZoneOffset.UTC);

    private final InterviewRepository interviewRepository;
    private final NotificationService notificationService;

    @Scheduled(cron = "${interview.reminders.cron:0 */15 * * * *}") // every 15 min by default
    @Transactional
    public void createReminders() {
        Instant now = Instant.now();
        createReminder24h(now);
        createReminder1h(now);
    }

    private void createReminder24h(Instant now) {
        Instant from = now.plus(Duration.ofHours(24)).minus(WINDOW_24H);
        Instant to = now.plus(Duration.ofHours(24)).plus(WINDOW_24H);
        List<Interview> interviews = interviewRepository.findByStatusAndStartAtBetween(InterviewStatus.CONFIRMED, from, to);
        for (Interview i : interviews) {
            createReminderForInterview(i, NotificationType.REMINDER_24H);
        }
    }

    private void createReminder1h(Instant now) {
        Instant from = now.plus(Duration.ofHours(1)).minus(WINDOW_1H);
        Instant to = now.plus(Duration.ofHours(1)).plus(WINDOW_1H);
        List<Interview> interviews = interviewRepository.findByStatusAndStartAtBetween(InterviewStatus.CONFIRMED, from, to);
        for (Interview i : interviews) {
            createReminderForInterview(i, NotificationType.REMINDER_1H);
        }
    }

    private void createReminderForInterview(Interview i, NotificationType type) {
        String msg = type == NotificationType.REMINDER_24H
                ? "Rappel : entretien dans 24h (" + TIME_FORMAT.format(i.getStartAt()) + ")."
                : "Rappel : entretien dans 1h (" + TIME_FORMAT.format(i.getStartAt()) + ").";
        notificationService.createReminderIfAbsent(i.getFreelancerId(), i.getId(), type, msg);
        notificationService.createReminderIfAbsent(i.getOwnerId(), i.getId(), type, msg);
    }
}
