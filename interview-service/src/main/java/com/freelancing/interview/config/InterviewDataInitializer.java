package com.freelancing.interview.config;

import com.freelancing.interview.entity.AvailabilitySlot;
import com.freelancing.interview.entity.Interview;
import com.freelancing.interview.entity.Notification;
import com.freelancing.interview.entity.Review;
import com.freelancing.interview.enums.InterviewStatus;
import com.freelancing.interview.enums.MeetingMode;
import com.freelancing.interview.enums.NotificationType;
import com.freelancing.interview.repository.AvailabilitySlotRepository;
import com.freelancing.interview.repository.InterviewRepository;
import com.freelancing.interview.repository.NotificationRepository;
import com.freelancing.interview.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

/**
 * Seeds availability slots, interviews (many rows), reviews, and notifications.
 * Clients: owner_id 2 (Sarra), 10 (Chaker), 11 (Salma). Freelancers: 3..9.
 * Fiabilité varies: clients 10 high, 11 low; freelancers 3/4/5/6/7/8/9 have distinct scores.
 */
@Configuration
@RequiredArgsConstructor
public class InterviewDataInitializer implements CommandLineRunner {

    private final AvailabilitySlotRepository slotRepository;
    private final InterviewRepository interviewRepository;
    private final ReviewRepository reviewRepository;
    private final NotificationRepository notificationRepository;

    private static final long OWNER_ID = 2L;   // Sarra Ben Ammar
    private static final long OWNER_ID_2 = 10L; // Chaker Mourad (high client fiabilité)
    private static final long OWNER_ID_3 = 11L; // Salma Ferchichi (low client fiabilité)
    private static final long FL3 = 3L;        // Amira Ben Ali
    private static final long FL4 = 4L;        // Youssef Trabelsi
    private static final long FL5 = 5L;        // Nadia Jlassi
    private static final long FL6 = 6L;        // Omar Khelifi
    private static final long FL7 = 7L;        // Ines Hammami
    private static final long FL8 = 8L;        // Mehdi Gharbi
    private static final long FL9 = 9L;        // Leila Mansour

    @Override
    @Transactional
    public void run(String... args) {
        if (interviewRepository.count() > 0 || slotRepository.count() > 0) {
            return;
        }

        Instant now = Instant.now().truncatedTo(ChronoUnit.MINUTES);
        ZonedDateTime base = now.atZone(ZoneOffset.UTC);

        // ---- Availability slots: freelancers 3..9 - next 14 days ----
        for (int day = 0; day < 14; day++) {
            ZonedDateTime d = base.plusDays(day);
            saveSlot(FL3, d.withHour(9).withMinute(0).toInstant(), d.withHour(12).withMinute(0).toInstant());
            saveSlot(FL3, d.withHour(14).withMinute(0).toInstant(), d.withHour(17).withMinute(0).toInstant());
            saveSlot(FL4, d.withHour(10).withMinute(0).toInstant(), d.withHour(13).withMinute(0).toInstant());
            saveSlot(FL5, d.withHour(9).withMinute(30).toInstant(), d.withHour(16).withMinute(0).toInstant());
            saveSlot(FL6, d.withHour(9).withMinute(0).toInstant(), d.withHour(12).withMinute(0).toInstant());
            saveSlot(FL7, d.withHour(14).withMinute(0).toInstant(), d.withHour(18).withMinute(0).toInstant());
            saveSlot(FL8, d.withHour(10).withMinute(0).toInstant(), d.withHour(15).withMinute(0).toInstant());
            saveSlot(FL9, d.withHour(8).withMinute(0).toInstant(), d.withHour(14).withMinute(0).toInstant());
        }

        // ---- 10 interviews: real-life Tunisian data, mixed freelancers ----
        // Freelancer 3 (Amira): 4 COMPLETED, 1 NO_SHOW, 1 CANCELLED -> fiabilité ~0.45
        Interview i1 = interview(FL3, OWNER_ID, now.minus(10, ChronoUnit.DAYS).plus(10, ChronoUnit.HOURS), 45, MeetingMode.ONLINE, InterviewStatus.COMPLETED, 1L, 1L, null);
        i1.setMeetingUrl("https://meet.jit.si/entretien-sfax-1");
        interviewRepository.save(i1);
        review(i1.getId(), OWNER_ID, FL3, 5, "Amira très professionnelle, compétences techniques solides.");

        Interview i2 = interview(FL3, OWNER_ID, now.minus(7, ChronoUnit.DAYS).plus(14, ChronoUnit.HOURS), 30, MeetingMode.FACE_TO_FACE, InterviewStatus.COMPLETED, 2L, 2L, null);
        i2.setAddressLine("45 Rue de la République");
        i2.setCity("Sfax");
        i2.setLat(34.7406);
        i2.setLng(10.7603);
        interviewRepository.save(i2);
        review(i2.getId(), OWNER_ID, FL3, 4, "Bon échange, ponctuelle.");

        Interview i3 = interview(FL3, OWNER_ID, now.minus(5, ChronoUnit.DAYS).plus(11, ChronoUnit.HOURS), 60, MeetingMode.ONLINE, InterviewStatus.COMPLETED, 3L, 3L, null);
        i3.setMeetingUrl("https://meet.jit.si/entretien-sfax-2");
        interviewRepository.save(i3);
        review(i3.getId(), OWNER_ID, FL3, 5, "Parfait, embauche prévue.");

        Interview i4 = interview(FL3, OWNER_ID, now.minus(3, ChronoUnit.DAYS).plus(15, ChronoUnit.HOURS), 30, MeetingMode.ONLINE, InterviewStatus.COMPLETED, null, null, null);
        i4.setMeetingUrl(null);
        interviewRepository.save(i4);

        Interview i5 = interview(FL3, OWNER_ID, now.minus(2, ChronoUnit.DAYS).plus(9, ChronoUnit.HOURS), 45, MeetingMode.FACE_TO_FACE, InterviewStatus.NO_SHOW, null, null, null);
        i5.setAddressLine("12 Avenue Habib Bourguiba");
        i5.setCity("Tunis");
        interviewRepository.save(i5);

        Interview i6 = interview(FL3, OWNER_ID, now.plus(4, ChronoUnit.DAYS).plus(10, ChronoUnit.HOURS), 30, MeetingMode.ONLINE, InterviewStatus.CANCELLED, null, null, null);
        interviewRepository.save(i6);

        // Freelancer 4 (Youssef): 2 COMPLETED -> good fiabilité
        Interview i7 = interview(FL4, OWNER_ID, now.minus(6, ChronoUnit.DAYS).plus(10, ChronoUnit.HOURS), 30, MeetingMode.ONLINE, InterviewStatus.COMPLETED, 4L, 4L, null);
        i7.setMeetingUrl("https://meet.jit.si/entretien-sousse-1");
        interviewRepository.save(i7);
        review(i7.getId(), OWNER_ID, FL4, 5, "Youssef réactif et compétent.");

        Interview i8 = interview(FL4, OWNER_ID, now.minus(1, ChronoUnit.DAYS).plus(14, ChronoUnit.HOURS), 45, MeetingMode.FACE_TO_FACE, InterviewStatus.COMPLETED, 5L, 5L, null);
        i8.setAddressLine("8 Rue Hedi Chaker");
        i8.setCity("Sousse");
        i8.setLat(35.8256);
        i8.setLng(10.6346);
        interviewRepository.save(i8);
        review(i8.getId(), OWNER_ID, FL4, 4, "Très bon contact.");

        // Freelancer 5 (Nadia): 1 CONFIRMED (upcoming), 1 PROPOSED
        Interview i9 = interview(FL5, OWNER_ID, now.plus(2, ChronoUnit.DAYS).plus(11, ChronoUnit.HOURS), 60, MeetingMode.ONLINE, InterviewStatus.CONFIRMED, 6L, 6L, null);
        i9.setMeetingUrl(null);
        interviewRepository.save(i9);

        Interview i10 = interview(FL6, OWNER_ID, now.plus(5, ChronoUnit.DAYS).plus(9, ChronoUnit.HOURS), 30, MeetingMode.ONLINE, InterviewStatus.PROPOSED, 7L, 7L, null);
        i10.setMeetingUrl("https://meet.jit.si/entretien-tunis-1");
        interviewRepository.save(i10);

        // ---- Client 10 (Chaker): 6 COMPLETED, 1 CANCELLED -> high client fiabilité ----
        Interview c10_1 = interview(FL3, OWNER_ID_2, now.minus(20, ChronoUnit.DAYS).plus(10, ChronoUnit.HOURS), 45, MeetingMode.ONLINE, InterviewStatus.COMPLETED, null, null, null);
        interviewRepository.save(c10_1);
        review(c10_1.getId(), OWNER_ID_2, FL3, 5, "Très bien.");
        Interview c10_2 = interview(FL4, OWNER_ID_2, now.minus(18, ChronoUnit.DAYS).plus(14, ChronoUnit.HOURS), 30, MeetingMode.ONLINE, InterviewStatus.COMPLETED, null, null, null);
        interviewRepository.save(c10_2);
        Interview c10_3 = interview(FL5, OWNER_ID_2, now.minus(15, ChronoUnit.DAYS).plus(11, ChronoUnit.HOURS), 45, MeetingMode.ONLINE, InterviewStatus.COMPLETED, null, null, null);
        interviewRepository.save(c10_3);
        Interview c10_4 = interview(FL6, OWNER_ID_2, now.minus(12, ChronoUnit.DAYS).plus(9, ChronoUnit.HOURS), 60, MeetingMode.ONLINE, InterviewStatus.COMPLETED, null, null, null);
        interviewRepository.save(c10_4);
        Interview c10_5 = interview(FL7, OWNER_ID_2, now.minus(9, ChronoUnit.DAYS).plus(15, ChronoUnit.HOURS), 30, MeetingMode.ONLINE, InterviewStatus.COMPLETED, null, null, null);
        interviewRepository.save(c10_5);
        Interview c10_6 = interview(FL8, OWNER_ID_2, now.minus(6, ChronoUnit.DAYS).plus(10, ChronoUnit.HOURS), 45, MeetingMode.ONLINE, InterviewStatus.COMPLETED, null, null, null);
        interviewRepository.save(c10_6);
        Interview c10_7 = interview(FL9, OWNER_ID_2, now.minus(4, ChronoUnit.DAYS).plus(14, ChronoUnit.HOURS), 30, MeetingMode.ONLINE, InterviewStatus.CANCELLED, null, null, null);
        interviewRepository.save(c10_7);

        // ---- Client 11 (Salma): 2 COMPLETED, 3 NO_SHOW, 2 CANCELLED -> low client fiabilité ----
        Interview c11_1 = interview(FL3, OWNER_ID_3, now.minus(19, ChronoUnit.DAYS).plus(10, ChronoUnit.HOURS), 45, MeetingMode.ONLINE, InterviewStatus.NO_SHOW, null, null, null);
        interviewRepository.save(c11_1);
        Interview c11_2 = interview(FL4, OWNER_ID_3, now.minus(17, ChronoUnit.DAYS).plus(14, ChronoUnit.HOURS), 30, MeetingMode.ONLINE, InterviewStatus.COMPLETED, null, null, null);
        interviewRepository.save(c11_2);
        Interview c11_3 = interview(FL5, OWNER_ID_3, now.minus(14, ChronoUnit.DAYS).plus(11, ChronoUnit.HOURS), 45, MeetingMode.ONLINE, InterviewStatus.NO_SHOW, null, null, null);
        interviewRepository.save(c11_3);
        Interview c11_4 = interview(FL6, OWNER_ID_3, now.minus(11, ChronoUnit.DAYS).plus(9, ChronoUnit.HOURS), 60, MeetingMode.ONLINE, InterviewStatus.COMPLETED, null, null, null);
        interviewRepository.save(c11_4);
        Interview c11_5 = interview(FL7, OWNER_ID_3, now.minus(8, ChronoUnit.DAYS).plus(15, ChronoUnit.HOURS), 30, MeetingMode.ONLINE, InterviewStatus.NO_SHOW, null, null, null);
        interviewRepository.save(c11_5);
        Interview c11_6 = interview(FL8, OWNER_ID_3, now.minus(5, ChronoUnit.DAYS).plus(10, ChronoUnit.HOURS), 45, MeetingMode.ONLINE, InterviewStatus.CANCELLED, null, null, null);
        interviewRepository.save(c11_6);
        Interview c11_7 = interview(FL9, OWNER_ID_3, now.minus(3, ChronoUnit.DAYS).plus(14, ChronoUnit.HOURS), 30, MeetingMode.ONLINE, InterviewStatus.CANCELLED, null, null, null);
        interviewRepository.save(c11_7);

        // ---- Extra past interviews: diversify freelancer fiabilité (owner = Sarra, client 2) ----
        // FL5: +2 COMPLETED, +1 CANCELLED -> total past 3 completed, 1 no_show, 1 cancelled
        interviewRepository.save(interview(FL5, OWNER_ID, now.minus(25, ChronoUnit.DAYS).plus(10, ChronoUnit.HOURS), 45, MeetingMode.ONLINE, InterviewStatus.COMPLETED, null, null, null));
        interviewRepository.save(interview(FL5, OWNER_ID, now.minus(22, ChronoUnit.DAYS).plus(14, ChronoUnit.HOURS), 30, MeetingMode.ONLINE, InterviewStatus.COMPLETED, null, null, null));
        interviewRepository.save(interview(FL5, OWNER_ID, now.minus(19, ChronoUnit.DAYS).plus(11, ChronoUnit.HOURS), 45, MeetingMode.ONLINE, InterviewStatus.CANCELLED, null, null, null));
        // FL6: +3 COMPLETED -> total 5 completed (with client 10 + 11)
        interviewRepository.save(interview(FL6, OWNER_ID, now.minus(24, ChronoUnit.DAYS).plus(9, ChronoUnit.HOURS), 45, MeetingMode.ONLINE, InterviewStatus.COMPLETED, null, null, null));
        interviewRepository.save(interview(FL6, OWNER_ID, now.minus(21, ChronoUnit.DAYS).plus(15, ChronoUnit.HOURS), 30, MeetingMode.ONLINE, InterviewStatus.COMPLETED, null, null, null));
        interviewRepository.save(interview(FL6, OWNER_ID, now.minus(16, ChronoUnit.DAYS).plus(10, ChronoUnit.HOURS), 60, MeetingMode.ONLINE, InterviewStatus.COMPLETED, null, null, null));
        // FL7: +1 NO_SHOW, +2 CANCELLED -> total 1 completed, 2 no_show, 2 cancelled
        interviewRepository.save(interview(FL7, OWNER_ID, now.minus(23, ChronoUnit.DAYS).plus(11, ChronoUnit.HOURS), 30, MeetingMode.ONLINE, InterviewStatus.NO_SHOW, null, null, null));
        interviewRepository.save(interview(FL7, OWNER_ID, now.minus(20, ChronoUnit.DAYS).plus(14, ChronoUnit.HOURS), 45, MeetingMode.ONLINE, InterviewStatus.CANCELLED, null, null, null));
        interviewRepository.save(interview(FL7, OWNER_ID, now.minus(17, ChronoUnit.DAYS).plus(9, ChronoUnit.HOURS), 30, MeetingMode.ONLINE, InterviewStatus.CANCELLED, null, null, null));
        // FL8: +3 COMPLETED, +1 NO_SHOW -> total 4 completed, 1 no_show, 1 cancelled
        interviewRepository.save(interview(FL8, OWNER_ID, now.minus(26, ChronoUnit.DAYS).plus(10, ChronoUnit.HOURS), 45, MeetingMode.ONLINE, InterviewStatus.COMPLETED, null, null, null));
        interviewRepository.save(interview(FL8, OWNER_ID, now.minus(23, ChronoUnit.DAYS).plus(14, ChronoUnit.HOURS), 30, MeetingMode.ONLINE, InterviewStatus.COMPLETED, null, null, null));
        interviewRepository.save(interview(FL8, OWNER_ID, now.minus(18, ChronoUnit.DAYS).plus(11, ChronoUnit.HOURS), 60, MeetingMode.ONLINE, InterviewStatus.COMPLETED, null, null, null));
        interviewRepository.save(interview(FL8, OWNER_ID, now.minus(14, ChronoUnit.DAYS).plus(9, ChronoUnit.HOURS), 45, MeetingMode.ONLINE, InterviewStatus.NO_SHOW, null, null, null));
        // FL9: +2 COMPLETED, +1 CANCELLED -> total 2 completed, 3 cancelled
        interviewRepository.save(interview(FL9, OWNER_ID, now.minus(27, ChronoUnit.DAYS).plus(10, ChronoUnit.HOURS), 30, MeetingMode.ONLINE, InterviewStatus.COMPLETED, null, null, null));
        interviewRepository.save(interview(FL9, OWNER_ID, now.minus(24, ChronoUnit.DAYS).plus(15, ChronoUnit.HOURS), 45, MeetingMode.ONLINE, InterviewStatus.COMPLETED, null, null, null));
        interviewRepository.save(interview(FL9, OWNER_ID, now.minus(21, ChronoUnit.DAYS).plus(11, ChronoUnit.HOURS), 30, MeetingMode.ONLINE, InterviewStatus.CANCELLED, null, null, null));

        // Notifications (freelancer-only for proposed)
        notification(FL6, i10.getId(), NotificationType.INTERVIEW_PROPOSED, "Nouvelle proposition d'entretien.");
        notification(OWNER_ID, i9.getId(), NotificationType.INTERVIEW_CONFIRMED, "Entretien confirmé avec le freelancer.");
        notification(FL5, i9.getId(), NotificationType.INTERVIEW_CONFIRMED, "Votre entretien a été confirmé.");
    }

    private AvailabilitySlot saveSlot(long freelancerId, Instant start, Instant end) {
        AvailabilitySlot s = new AvailabilitySlot(null, freelancerId, start, end, false, null);
        return slotRepository.save(s);
    }

    private Interview interview(long freelancerId, long ownerId, Instant start, int durationMinutes, MeetingMode mode, InterviewStatus status, Long projectId, Long candidatureId, Long slotId) {
        Interview i = new Interview();
        i.setFreelancerId(freelancerId);
        i.setOwnerId(ownerId);
        i.setStartAt(start);
        i.setEndAt(start.plus(durationMinutes, ChronoUnit.MINUTES));
        i.setMode(mode);
        i.setStatus(status);
        i.setProjectId(projectId);
        i.setCandidatureId(candidatureId);
        i.setSlotId(slotId);
        return i;
    }

    private void review(long interviewId, long reviewerId, long revieweeId, int score, String comment) {
        Review r = new Review();
        r.setInterviewId(interviewId);
        r.setReviewerId(reviewerId);
        r.setRevieweeId(revieweeId);
        r.setScore(score);
        r.setComment(comment);
        reviewRepository.save(r);
    }

    private void notification(long userId, long interviewId, NotificationType type, String message) {
        Notification n = new Notification();
        n.setUserId(userId);
        n.setInterviewId(interviewId);
        n.setType(type);
        n.setMessage(message);
        n.setReadAt(null);
        notificationRepository.save(n);
    }
}
