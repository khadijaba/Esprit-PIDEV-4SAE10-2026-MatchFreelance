package com.freelancing.interview.config;

import com.freelancing.interview.entity.AvailabilitySlot;
import com.freelancing.interview.entity.Interview;
import com.freelancing.interview.entity.Review;
import com.freelancing.interview.enums.InterviewStatus;
import com.freelancing.interview.enums.MeetingMode;
import com.freelancing.interview.repository.AvailabilitySlotRepository;
import com.freelancing.interview.repository.InterviewRepository;
import com.freelancing.interview.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

@Configuration
@RequiredArgsConstructor
public class InterviewDataInitializer implements CommandLineRunner {

    private final AvailabilitySlotRepository slotRepository;
    private final InterviewRepository interviewRepository;
    private final ReviewRepository reviewRepository;

    @Override
    @Transactional
    public void run(String... args) {
        if (interviewRepository.count() > 0 || slotRepository.count() > 0) {
            // Do not overwrite existing data if the DB is already used
            return;
        }

        // Use freelancerId = 1 and ownerId = 1/2 to match Angular hardcoded IDs.
        Long freelancerId = 1L;
        Long owner1 = 1L;
        Long owner2 = 2L;

        Instant now = Instant.now().truncatedTo(ChronoUnit.MINUTES);
        ZonedDateTime baseUtc = now.atZone(ZoneOffset.UTC);

        // --- Availability slots for freelancer 1 ---
        ZonedDateTime dayPlus2 = baseUtc.plusDays(2).withHour(10).withMinute(0);
        ZonedDateTime dayPlus3 = baseUtc.plusDays(3).withHour(14).withMinute(0);

        AvailabilitySlot slot1 = new AvailabilitySlot(
                null,
                freelancerId,
                dayPlus2.toInstant(),
                dayPlus2.plusMinutes(30).toInstant(),
                false,
                null
        );

        AvailabilitySlot slot2 = new AvailabilitySlot(
                null,
                freelancerId,
                dayPlus3.toInstant(),
                dayPlus3.plusMinutes(60).toInstant(),
                false,
                null
        );

        slot1 = slotRepository.save(slot1);
        slot2 = slotRepository.save(slot2);

        // --- Interviews ---

        // I1: PROPOSED interview in 2 days (owner1, freelancer1) - can be confirmed/cancelled from UI.
        Interview i1 = new Interview();
        i1.setFreelancerId(freelancerId);
        i1.setOwnerId(owner1);
        i1.setStartAt(now.plus(2, ChronoUnit.DAYS).plus(10, ChronoUnit.HOURS));
        i1.setEndAt(now.plus(2, ChronoUnit.DAYS).plus(10, ChronoUnit.HOURS).plus(30, ChronoUnit.MINUTES));
        i1.setMode(MeetingMode.ONLINE);
        i1.setMeetingUrl("https://meet.example.com/seed-proposed");
        i1.setStatus(InterviewStatus.PROPOSED);
        Interview saved1 = interviewRepository.save(i1);

        // I2: CONFIRMED interview in 12 hours (owner1, freelancer1) - used to test cancellation policy (<24h).
        Interview i2 = new Interview();
        i2.setFreelancerId(freelancerId);
        i2.setOwnerId(owner1);
        i2.setStartAt(now.plus(12, ChronoUnit.HOURS));
        i2.setEndAt(now.plus(12, ChronoUnit.HOURS).plus(30, ChronoUnit.MINUTES));
        i2.setMode(MeetingMode.ONLINE);
        i2.setMeetingUrl("https://meet.example.com/seed-confirmed");
        i2.setStatus(InterviewStatus.CONFIRMED);
        Interview saved2 = interviewRepository.save(i2);

        // I3: COMPLETED interview from yesterday (owner2 reviews freelancer1).
        Interview i3 = new Interview();
        i3.setFreelancerId(freelancerId);
        i3.setOwnerId(owner2);
        i3.setStartAt(now.minus(1, ChronoUnit.DAYS).plus(9, ChronoUnit.HOURS));
        i3.setEndAt(now.minus(1, ChronoUnit.DAYS).plus(9, ChronoUnit.HOURS).plus(30, ChronoUnit.MINUTES));
        i3.setMode(MeetingMode.FACE_TO_FACE);
        i3.setAddressLine("123 Seed Street");
        i3.setCity("Seed City");
        i3.setStatus(InterviewStatus.COMPLETED);
        Interview saved3 = interviewRepository.save(i3);

        // Seed one review: owner2 -> freelancer1 for I3.
        Review review = new Review();
        review.setInterviewId(saved3.getId());
        review.setReviewerId(owner2);
        review.setRevieweeId(freelancerId);
        review.setScore(5);
        review.setComment("Seed review: excellent collaboration.");
        reviewRepository.save(review);
    }
}

