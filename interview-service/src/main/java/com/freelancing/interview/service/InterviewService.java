package com.freelancing.interview.service;

import com.freelancing.interview.dto.AlternativeSlotSuggestionDTO;
import com.freelancing.interview.dto.InterviewCreateRequestDTO;
import com.freelancing.interview.dto.InterviewResponseDTO;
import com.freelancing.interview.dto.InterviewUpdateRequestDTO;
import com.freelancing.interview.dto.ReliabilityResponseDTO;
import com.freelancing.interview.dto.TopFreelancerInInterviewsDTO;
import com.freelancing.interview.dto.VisioRoomResponseDTO;
import com.freelancing.interview.dto.WorkloadSummaryDTO;
import com.freelancing.interview.entity.AvailabilitySlot;
import com.freelancing.interview.entity.Interview;
import com.freelancing.interview.enums.InterviewStatus;
import com.freelancing.interview.enums.MeetingMode;
import com.freelancing.interview.repository.AvailabilitySlotRepository;
import com.freelancing.interview.enums.NotificationType;
import com.freelancing.interview.repository.InterviewRepository;
import com.freelancing.interview.repository.InterviewSpecifications;
import com.freelancing.interview.repository.ReviewRepository;
import com.freelancing.interview.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class InterviewService {

    private static final DateTimeFormatter ICS_DATE_TIME_UTC =
            DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'").withZone(ZoneOffset.UTC);

    @Value("${interview.cancellation.allowed-hours-before:24}")
    private int cancellationAllowedHoursBefore;

    @Value("${jitsi.base-url:https://meet.jit.si}")
    private String jitsiBaseUrl;

    private final InterviewRepository interviewRepository;
    private final AvailabilitySlotRepository slotRepository;
    private final ReviewRepository reviewRepository;
    private final NotificationService notificationService;

    private static final double REVIEW_NEUTRAL = 0.5;
    private static final double COMBINED_WEIGHT_RELIABILITY = 0.5;
    private static final double COMBINED_WEIGHT_REVIEW = 0.5;

    @Transactional
    public InterviewResponseDTO create(InterviewCreateRequestDTO req) {
        // One interview per freelancer per application/project.
        // If the UI creates interviews from candidatures, candidatureId is the strongest key.
        if (req.getCandidatureId() != null && interviewRepository.existsByCandidatureId(req.getCandidatureId())) {
            throw new IllegalStateException("An interview already exists for this application (candidature)");
        }
        if (req.getProjectId() != null && interviewRepository.existsByProjectIdAndFreelancerId(req.getProjectId(), req.getFreelancerId())) {
            throw new IllegalStateException("You can only create one interview per freelancer for this project");
        }

        Instant startAt;
        Instant endAt;
        Long slotId = null;

        if (req.getSlotId() != null) {
            AvailabilitySlot slot = slotRepository.findById(req.getSlotId())
                    .orElseThrow(() -> new RuntimeException("Availability slot not found with id: " + req.getSlotId()));
            if (!slot.getFreelancerId().equals(req.getFreelancerId())) {
                throw new IllegalArgumentException("slot does not belong to freelancer " + req.getFreelancerId());
            }
            if (slot.isBooked()) {
                throw new IllegalStateException("slot already booked");
            }
            slotId = slot.getId();
            startAt = slot.getStartAt();
            // If durationMinutes is provided, shorten the meeting within the slot bounds.
            if (req.getDurationMinutes() != null && req.getDurationMinutes() > 0) {
                endAt = startAt.plus(req.getDurationMinutes(), ChronoUnit.MINUTES);
                if (!endAt.isAfter(startAt)) {
                    throw new IllegalArgumentException("durationMinutes must result in a positive time range");
                }
                if (endAt.isAfter(slot.getEndAt())) {
                    throw new IllegalArgumentException("Chosen duration exceeds the freelancer availability slot");
                }
            } else {
                endAt = slot.getEndAt();
            }
        } else {
            if (req.getStartAt() == null || req.getEndAt() == null) {
                throw new IllegalArgumentException("Provide either slotId or (startAt and endAt)");
            }
            startAt = req.getStartAt();
            endAt = req.getEndAt();

            // When creating from date/time + duration on the calendar, ensure the chosen time window
            // is fully contained in at least one free availability slot for this freelancer.
            AvailabilitySlot containing = slotRepository
                    .findFirstByFreelancerIdAndBookedFalseAndStartAtLessThanEqualAndEndAtGreaterThanEqual(
                            req.getFreelancerId(), startAt, endAt)
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Freelancer isn't available at this date/time for the chosen duration. " +
                            "Pick another time/date or reduce duration based on the freelancer's availability."));
            slotId = containing.getId();
        }

        if (!endAt.isAfter(startAt)) {
            throw new IllegalArgumentException("endAt must be after startAt");
        }

        // Prevent overlaps for freelancer and owner (excluding CANCELLED/NO_SHOW).
        assertNoScheduleConflicts(req.getFreelancerId(), req.getOwnerId(), startAt, endAt, null);

        MeetingMode mode = req.getMode() != null ? req.getMode() : MeetingMode.ONLINE;
        validateMeetingFields(mode, req.getMeetingUrl(), req.getAddressLine(), req.getCity());

        Interview interview = new Interview();
        interview.setCandidatureId(req.getCandidatureId());
        interview.setProjectId(req.getProjectId());
        interview.setFreelancerId(req.getFreelancerId());
        interview.setOwnerId(req.getOwnerId());
        interview.setSlotId(slotId);
        interview.setStartAt(startAt);
        interview.setEndAt(endAt);
        interview.setMode(mode);
        interview.setMeetingUrl(trimToNull(req.getMeetingUrl()));
        interview.setAddressLine(trimToNull(req.getAddressLine()));
        interview.setCity(trimToNull(req.getCity()));
        interview.setLat(req.getLat());
        interview.setLng(req.getLng());
        interview.setNotes(trimToNull(req.getNotes()));
        interview.setStatus(InterviewStatus.PROPOSED);

        Interview saved = interviewRepository.save(interview);

        // Only notify the freelancer when the owner creates a proposed interview (owner is the creator).
        notificationService.emitToFreelancerOnly(saved, NotificationType.INTERVIEW_PROPOSED);

        if (slotId != null) {
            AvailabilitySlot slot = slotRepository.findById(slotId).orElseThrow();
            slot.setBooked(true);
            slot.setBookedInterviewId(saved.getId());
            slotRepository.save(slot);
        }

        return toDto(saved);
    }

    @Transactional(readOnly = true)
    public List<AlternativeSlotSuggestionDTO> suggestAlternatives(InterviewCreateRequestDTO req) {
        if (req.getFreelancerId() == null) {
            throw new IllegalArgumentException("freelancerId is required for suggestions");
        }
        if (req.getOwnerId() == null) {
            throw new IllegalArgumentException("ownerId is required for suggestions");
        }

        Instant preferredStart = req.getStartAt();
        Instant preferredEnd = req.getEndAt();
        if (preferredStart == null || preferredEnd == null || !preferredEnd.isAfter(preferredStart)) {
            throw new IllegalArgumentException("Valid startAt and endAt are required for suggestions");
        }
        Duration desiredDuration = Duration.between(preferredStart, preferredEnd);

        Instant now = Instant.now();
        Instant searchFrom = preferredStart.isAfter(now) ? preferredStart : now;
        Instant searchTo = preferredStart.plus(14, ChronoUnit.DAYS);

        List<AvailabilitySlot> slots = slotRepository
                .findAllByFreelancerIdAndBookedFalseAndStartAtGreaterThanEqualAndEndAtLessThanEqual(
                        req.getFreelancerId(), searchFrom, searchTo);

        List<AlternativeSlotSuggestionDTO> suggestions = new ArrayList<>();
        for (AvailabilitySlot slot : slots) {
            Duration slotDuration = Duration.between(slot.getStartAt(), slot.getEndAt());
            if (slotDuration.compareTo(desiredDuration) < 0) {
                continue;
            }

            Instant latestStart = slot.getEndAt().minus(desiredDuration);
            Instant candidateStart = preferredStart;
            if (candidateStart.isBefore(slot.getStartAt())) {
                candidateStart = slot.getStartAt();
            }
            if (candidateStart.isAfter(latestStart)) {
                candidateStart = latestStart;
            }
            Instant candidateEnd = candidateStart.plus(desiredDuration);
            if (!candidateEnd.isAfter(candidateStart) || candidateEnd.isAfter(slot.getEndAt())) {
                continue;
            }

            if (hasScheduleConflicts(req.getFreelancerId(), req.getOwnerId(), candidateStart, candidateEnd, null)) {
                continue;
            }

            long score = computeSuggestionScore(preferredStart, candidateStart);
            suggestions.add(new AlternativeSlotSuggestionDTO(candidateStart, candidateEnd, slot.getId(), score));
        }

        suggestions.sort(Comparator.comparingLong(AlternativeSlotSuggestionDTO::getScore));
        return suggestions.size() > 5 ? suggestions.subList(0, 5) : suggestions;
    }

    @Transactional(readOnly = true)
    public Page<InterviewResponseDTO> search(
            Long freelancerId,
            Long ownerId,
            Long projectId,
            Long candidatureId,
            InterviewStatus status,
            MeetingMode mode,
            Instant from,
            Instant to,
            Pageable pageable
    ) {
        Specification<Interview> spec = Specification.where(InterviewSpecifications.hasFreelancerId(freelancerId))
                .and(InterviewSpecifications.hasOwnerId(ownerId))
                .and(InterviewSpecifications.hasProjectId(projectId))
                .and(InterviewSpecifications.hasCandidatureId(candidatureId))
                .and(InterviewSpecifications.hasStatus(status))
                .and(InterviewSpecifications.hasMode(mode))
                .and(InterviewSpecifications.startAtGte(from))
                .and(InterviewSpecifications.startAtLte(to));

        return interviewRepository.findAll(spec, pageable).map(this::toDto);
    }

    @Transactional
    public InterviewResponseDTO update(Long interviewId, InterviewUpdateRequestDTO req) {
        Interview interview = interviewRepository.findById(interviewId)
                .orElseThrow(() -> new RuntimeException("Interview not found with id: " + interviewId));

        if (req.getMode() != null) {
            interview.setMode(req.getMode());
        }
        if (req.getStatus() != null) {
            interview.setStatus(req.getStatus());
        }

        if (req.getStartAt() != null && req.getEndAt() != null) {
            if (interview.getSlotId() != null) {
                throw new IllegalStateException("Cannot reschedule an interview created from an availability slot. Pick a new slot.");
            }
            if (!req.getEndAt().isAfter(req.getStartAt())) {
                throw new IllegalArgumentException("endAt must be after startAt");
            }
            assertNoScheduleConflicts(
                    interview.getFreelancerId(),
                    interview.getOwnerId(),
                    req.getStartAt(),
                    req.getEndAt(),
                    interview.getId()
            );
            interview.setStartAt(req.getStartAt());
            interview.setEndAt(req.getEndAt());
        }

        if (req.getNotes() != null) interview.setNotes(trimToNull(req.getNotes()));
        if (req.getMeetingUrl() != null) interview.setMeetingUrl(trimToNull(req.getMeetingUrl()));
        if (req.getAddressLine() != null) interview.setAddressLine(trimToNull(req.getAddressLine()));
        if (req.getCity() != null) interview.setCity(trimToNull(req.getCity()));
        if (req.getLat() != null) interview.setLat(req.getLat());
        if (req.getLng() != null) interview.setLng(req.getLng());

        validateMeetingFields(interview.getMode(), interview.getMeetingUrl(), interview.getAddressLine(), interview.getCity());

        return toDto(interviewRepository.save(interview));
    }

    @Transactional
    public InterviewResponseDTO confirm(Long interviewId) {
        Interview interview = interviewRepository.findById(interviewId)
                .orElseThrow(() -> new RuntimeException("Interview not found with id: " + interviewId));
        if (interview.getStatus() == InterviewStatus.CANCELLED) {
            throw new IllegalStateException("Cannot confirm a cancelled interview");
        }
        interview.setStatus(InterviewStatus.CONFIRMED);
        Interview saved = interviewRepository.save(interview);
        notificationService.emitToBoth(saved, NotificationType.INTERVIEW_CONFIRMED);
        return toDto(saved);
    }

    @Transactional
    public InterviewResponseDTO cancel(Long interviewId) {
        Interview interview = interviewRepository.findById(interviewId)
                .orElseThrow(() -> new RuntimeException("Interview not found with id: " + interviewId));
        if (interview.getStatus() == InterviewStatus.CANCELLED) {
            throw new IllegalStateException("Interview is already cancelled");
        }
        Duration untilStart = Duration.between(Instant.now(), interview.getStartAt());
        if (untilStart.toHours() < cancellationAllowedHoursBefore) {
            throw new IllegalStateException(
                    "Annulation non autorisée : il reste moins de " + cancellationAllowedHoursBefore + " h avant l'entretien. Vous pouvez marquer l'entretien en 'Absence non excusée' (NO_SHOW) si le candidat ne s'est pas présenté.");
        }
        interview.setStatus(InterviewStatus.CANCELLED);

        if (interview.getSlotId() != null) {
            AvailabilitySlot slot = slotRepository.findById(interview.getSlotId()).orElse(null);
            if (slot != null) {
                slot.setBooked(false);
                slot.setBookedInterviewId(null);
                slotRepository.save(slot);
            }
        }

        Interview saved = interviewRepository.save(interview);
        notificationService.emitToBoth(saved, NotificationType.INTERVIEW_CANCELLED);
        return toDto(saved);
    }

    /**
     * Allow the freelancer to explicitly reject a proposed interview.
     * This is modeled as a cancellation without the time-based policy restriction.
     */
    @Transactional
    public InterviewResponseDTO reject(Long interviewId) {
        Interview interview = interviewRepository.findById(interviewId)
                .orElseThrow(() -> new RuntimeException("Interview not found with id: " + interviewId));
        if (interview.getStatus() != InterviewStatus.PROPOSED) {
            throw new IllegalStateException("Only proposed interviews can be rejected");
        }
        if (!Instant.now().isBefore(interview.getStartAt())) {
            throw new IllegalStateException("Cannot reject an interview that has already started");
        }

        interview.setStatus(InterviewStatus.CANCELLED);

        if (interview.getSlotId() != null) {
            AvailabilitySlot slot = slotRepository.findById(interview.getSlotId()).orElse(null);
            if (slot != null) {
                slot.setBooked(false);
                slot.setBookedInterviewId(null);
                slotRepository.save(slot);
            }
        }

        Interview saved = interviewRepository.save(interview);
        notificationService.emitToBoth(saved, NotificationType.INTERVIEW_CANCELLED);
        return toDto(saved);
    }

    /** Mark interview as NO_SHOW (e.g. when cancellation is no longer allowed and candidate did not show). */
    @Transactional
    public InterviewResponseDTO noShow(Long interviewId) {
        Interview interview = interviewRepository.findById(interviewId)
                .orElseThrow(() -> new RuntimeException("Interview not found with id: " + interviewId));
        if (interview.getStatus() == InterviewStatus.CANCELLED) {
            throw new IllegalStateException("Cannot set NO_SHOW on a cancelled interview");
        }
        if (Instant.now().isBefore(interview.getEndAt())) {
            throw new IllegalStateException("Vous ne pouvez marquer l'entretien en NO_SHOW qu'après l'heure de fin prévue.");
        }
        interview.setStatus(InterviewStatus.NO_SHOW);
        if (interview.getSlotId() != null) {
            AvailabilitySlot slot = slotRepository.findById(interview.getSlotId()).orElse(null);
            if (slot != null) {
                slot.setBooked(false);
                slot.setBookedInterviewId(null);
                slotRepository.save(slot);
            }
        }
        Interview saved = interviewRepository.save(interview);
        notificationService.emitToBoth(saved, NotificationType.INTERVIEW_NO_SHOW);
        return toDto(saved);
    }

    @Transactional(readOnly = true)
    public InterviewResponseDTO getById(Long interviewId) {
        Interview interview = interviewRepository.findById(interviewId)
                .orElseThrow(() -> new RuntimeException("Interview not found with id: " + interviewId));
        return toDto(interview);
    }

    /** Mark interview as COMPLETED. Allowed only after the scheduled end time. */
    @Transactional
    public InterviewResponseDTO complete(Long interviewId) {
        Interview interview = interviewRepository.findById(interviewId)
                .orElseThrow(() -> new RuntimeException("Interview not found with id: " + interviewId));
        if (interview.getStatus() != InterviewStatus.CONFIRMED) {
            throw new IllegalStateException("Seuls les entretiens confirmés peuvent être marqués comme terminés.");
        }
        if (Instant.now().isBefore(interview.getEndAt())) {
            throw new IllegalStateException("L'entretien ne peut être marqué terminé qu'après l'heure de fin prévue.");
        }
        interview.setStatus(InterviewStatus.COMPLETED);
        Interview saved = interviewRepository.save(interview);
        notificationService.emitToBoth(saved, NotificationType.INTERVIEW_COMPLETED);
        return toDto(saved);
    }

    /** Get or create in-app visio room for ONLINE interviews. Returns roomId and joinUrl (meetingUrl or embed URL). */
    @Transactional
    public VisioRoomResponseDTO getOrCreateVisioRoom(Long interviewId) {
        Interview interview = interviewRepository.findById(interviewId)
                .orElseThrow(() -> new RuntimeException("Interview not found with id: " + interviewId));
        if (interview.getMode() != MeetingMode.ONLINE) {
            throw new IllegalStateException("Visio room is only available for ONLINE interviews");
        }
        assertWithinAccessWindow(interview);
        String roomId = interview.getVisioRoomId();
        if (roomId == null || roomId.isBlank()) {
            roomId = "room-" + UUID.randomUUID();
            interview.setVisioRoomId(roomId);
            interviewRepository.save(interview);
        }
        String base = jitsiBaseUrl.endsWith("/") ? jitsiBaseUrl.substring(0, jitsiBaseUrl.length() - 1) : jitsiBaseUrl;
        String joinUrl = base + "/freelance-interview-" + roomId;
        return new VisioRoomResponseDTO(roomId, joinUrl);
    }

    private void assertWithinAccessWindow(Interview interview) {
        Instant now = Instant.now();
        if (now.isBefore(interview.getStartAt())) {
            throw new IllegalStateException("Online meeting is not available yet (before the scheduled start time)");
        }
        if (now.isAfter(interview.getEndAt())) {
            throw new IllegalStateException("Online meeting is no longer available (after the scheduled end time)");
        }
    }

    @Transactional(readOnly = true)
    public WorkloadSummaryDTO computeWorkloadForFreelancer(Long freelancerId) {
        if (freelancerId == null) {
            throw new IllegalArgumentException("freelancerId is required");
        }
        Instant now = Instant.now();
        Instant to = now.plus(7, ChronoUnit.DAYS);

        List<Interview> interviews = interviewRepository.findByFreelancerIdAndStartAtBetween(
                freelancerId, now, to);

        long totalMinutes7 = 0;
        long totalMinutes1 = 0;
        int n1 = 0;
        int n3 = 0;
        int n7 = interviews.size();

        Map<String, Long> dailyMinutes = new HashMap<>();

        Instant in24h = now.plus(1, ChronoUnit.DAYS);
        Instant in3d = now.plus(3, ChronoUnit.DAYS);

        for (Interview i : interviews) {
            if (i.getStatus() != InterviewStatus.CONFIRMED) {
                continue;
            }
            long minutes = Duration.between(i.getStartAt(), i.getEndAt()).toMinutes();
            if (minutes <= 0) continue;

            totalMinutes7 += minutes;

            if (!i.getStartAt().isAfter(in24h)) {
                totalMinutes1 += minutes;
                n1++;
            }
            if (!i.getStartAt().isAfter(in3d)) {
                n3++;
            }

            String dayKey = i.getStartAt().atZone(ZoneId.systemDefault()).toLocalDate().toString();
            dailyMinutes.merge(dayKey, minutes, Long::sum);
        }

        long maxDailyMinutes = dailyMinutes.values().stream().mapToLong(Long::longValue).max().orElse(0);

        String level;
        if (totalMinutes7 >= 600 || maxDailyMinutes >= 240) {
            level = "OVERLOADED";
        } else if (totalMinutes7 >= 360 || n1 >= 3) {
            level = "BUSY";
        } else if (totalMinutes7 >= 120) {
            level = "NORMAL";
        } else {
            level = "LIGHT";
        }

        return new WorkloadSummaryDTO(
                freelancerId,
                now,
                to,
                totalMinutes7,
                totalMinutes1,
                n1,
                n3,
                n7,
                maxDailyMinutes,
                level
        );
    }

    @Transactional(readOnly = true)
    public ReliabilityResponseDTO computeReliabilityForFreelancer(Long freelancerId, Instant from, Instant to) {
        if (freelancerId == null) {
            throw new IllegalArgumentException("freelancerId is required");
        }
        Instant effectiveTo = to != null ? to : Instant.now();
        Instant effectiveFrom = from != null ? from : effectiveTo.minus(180, ChronoUnit.DAYS);

        List<Interview> interviews = interviewRepository.findByFreelancerIdAndStartAtBetween(
                freelancerId, effectiveFrom, effectiveTo);
        return computeReliability("FREELANCER", freelancerId, interviews, effectiveFrom, effectiveTo);
    }

    @Transactional(readOnly = true)
    public ReliabilityResponseDTO computeReliabilityForOwner(Long ownerId, Instant from, Instant to) {
        if (ownerId == null) {
            throw new IllegalArgumentException("ownerId is required");
        }
        Instant effectiveTo = to != null ? to : Instant.now();
        Instant effectiveFrom = from != null ? from : effectiveTo.minus(180, ChronoUnit.DAYS);

        List<Interview> interviews = interviewRepository.findByOwnerIdAndStartAtBetween(
                ownerId, effectiveFrom, effectiveTo);
        return computeReliability("OWNER", ownerId, interviews, effectiveFrom, effectiveTo);
    }

    private ReliabilityResponseDTO computeReliability(
            String role,
            Long userId,
            List<Interview> interviews,
            Instant from,
            Instant to
    ) {
        int completed = 0;
        int noShow = 0;
        int cancelled = 0;

        for (Interview i : interviews) {
            if (i.getStatus() == InterviewStatus.COMPLETED) {
                completed++;
            } else if (i.getStatus() == InterviewStatus.NO_SHOW) {
                noShow++;
            } else if (i.getStatus() == InterviewStatus.CANCELLED) {
                cancelled++;
            }
        }

        int totalEvents = completed + noShow + cancelled;
        if (totalEvents == 0) {
            return new ReliabilityResponseDTO(userId, role, 0.5, 0, 0, 0, from, to);
        }

        double raw = (completed - noShow - 0.5 * cancelled) / Math.max(totalEvents, 1.0);
        raw = Math.max(0.0, Math.min(1.0, raw));

        double alpha = 3.0;
        double prior = 0.7;
        double score = (raw * totalEvents + prior * alpha) / (totalEvents + alpha);

        return new ReliabilityResponseDTO(userId, role, score, completed, noShow, cancelled, from, to);
    }

    @Transactional(readOnly = true)
    public List<TopFreelancerInInterviewsDTO> getTopFreelancersInInterviews(
            int limit,
            Long ownerId,
            int minReviews,
            Instant from,
            Instant to
    ) {
        Instant effectiveTo = to != null ? to : Instant.now();
        Instant effectiveFrom = from != null ? from : effectiveTo.minus(180, ChronoUnit.DAYS);

        List<Long> freelancerIds = ownerId != null
                ? interviewRepository.findDistinctFreelancerIdsWithInterviewsSinceAndOwnerId(effectiveFrom, ownerId)
                : interviewRepository.findDistinctFreelancerIdsWithInterviewsSince(effectiveFrom);

        if (freelancerIds.isEmpty()) {
            return List.of();
        }

        List<Object[]> reviewAggregates = reviewRepository.findAverageScoreAndCountByRevieweeIdIn(freelancerIds);
        Map<Long, double[]> reviewByFreelancer = new HashMap<>();
        for (Object[] row : reviewAggregates) {
            Long revieweeId = (Long) row[0];
            Double avgScore = (Double) row[1];
            Long cnt = (Long) row[2];
            if (revieweeId != null && avgScore != null && cnt != null) {
                double reviewNorm = (avgScore - 1.0) / 4.0;
                reviewByFreelancer.put(revieweeId, new double[]{reviewNorm, avgScore, cnt.doubleValue()});
            }
        }

        List<TopFreelancerInInterviewsDTO> results = new ArrayList<>();
        for (Long fid : freelancerIds) {
            ReliabilityResponseDTO rel = computeReliabilityForFreelancer(fid, effectiveFrom, effectiveTo);
            double reliabilityScore = rel.getScore();

            double reviewNorm = REVIEW_NEUTRAL;
            Double averageReviewScore = null;
            long reviewCount = 0;
            if (reviewByFreelancer.containsKey(fid)) {
                double[] arr = reviewByFreelancer.get(fid);
                reviewNorm = arr[0];
                averageReviewScore = arr[1];
                reviewCount = (long) arr[2];
            }

            if (minReviews > 0 && reviewCount < minReviews) {
                continue;
            }

            double combinedScore = COMBINED_WEIGHT_RELIABILITY * reliabilityScore + COMBINED_WEIGHT_REVIEW * reviewNorm;
            combinedScore = Math.max(0.0, Math.min(1.0, combinedScore));

            results.add(new TopFreelancerInInterviewsDTO(
                    fid,
                    combinedScore,
                    reliabilityScore,
                    averageReviewScore,
                    reviewCount,
                    rel.getCompletedCount(),
                    rel.getNoShowCount(),
                    rel.getCancelledCount()
            ));
        }

        results.sort(Comparator.comparingDouble(TopFreelancerInInterviewsDTO::getCombinedScore).reversed());
        return results.size() > limit ? results.subList(0, limit) : results;
    }

    /**
     * Generate a simple iCalendar (.ics) representation for this interview (UTC times).
     */
    @Transactional(readOnly = true)
    public String generateIcs(Long interviewId) {
        Interview interview = interviewRepository.findById(interviewId)
                .orElseThrow(() -> new RuntimeException("Interview not found with id: " + interviewId));

        String dtStart = ICS_DATE_TIME_UTC.format(interview.getStartAt());
        String dtEnd = ICS_DATE_TIME_UTC.format(interview.getEndAt());
        String uid = "interview-" + interview.getId() + "@freelancing-platform";

        StringBuilder sb = new StringBuilder();
        sb.append("BEGIN:VCALENDAR").append("\r\n");
        sb.append("VERSION:2.0").append("\r\n");
        sb.append("PRODID:-//FreelancingPlatform//InterviewService//EN").append("\r\n");
        sb.append("BEGIN:VEVENT").append("\r\n");
        sb.append("UID:").append(uid).append("\r\n");
        sb.append("DTSTAMP:").append(ICS_DATE_TIME_UTC.format(Instant.now())).append("\r\n");
        sb.append("DTSTART:").append(dtStart).append("\r\n");
        sb.append("DTEND:").append(dtEnd).append("\r\n");
        sb.append("SUMMARY:Interview").append("\r\n");

        if (interview.getMode() == MeetingMode.ONLINE && trimToNull(interview.getMeetingUrl()) != null) {
            sb.append("DESCRIPTION:Online interview").append("\\nURL:").append(interview.getMeetingUrl()).append("\r\n");
            sb.append("URL:").append(interview.getMeetingUrl()).append("\r\n");
        } else if (interview.getMode() == MeetingMode.FACE_TO_FACE) {
            StringBuilder location = new StringBuilder();
            if (trimToNull(interview.getAddressLine()) != null) {
                location.append(interview.getAddressLine());
            }
            if (trimToNull(interview.getCity()) != null) {
                if (!location.isEmpty()) {
                    location.append(", ");
                }
                location.append(interview.getCity());
            }
            if (!location.isEmpty()) {
                sb.append("LOCATION:").append(location).append("\r\n");
            }
        }

        sb.append("END:VEVENT").append("\r\n");
        sb.append("END:VCALENDAR").append("\r\n");

        return sb.toString();
    }

    /**
     * Export all not-completed interviews (PROPOSED, CONFIRMED) to Excel.
     * Optional ownerId and freelancerId filter the list.
     */
    @Transactional(readOnly = true)
    public byte[] exportNotCompletedInterviewsExcel(Long ownerId, Long freelancerId) {
        Specification<Interview> spec = Specification
                .where(InterviewSpecifications.statusIn(List.of(InterviewStatus.PROPOSED, InterviewStatus.CONFIRMED)))
                .and(InterviewSpecifications.hasOwnerId(ownerId))
                .and(InterviewSpecifications.hasFreelancerId(freelancerId));
        List<Interview> list = interviewRepository.findAll(spec, Pageable.unpaged()).getContent();

        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("Entretiens a venir");
            Row header = sheet.createRow(0);
            String[] headers = { "Id", "Début", "Fin", "Mode", "Statut", "Projet", "Freelancer", "Client", "URL réunion", "Adresse", "Ville" };
            for (int i = 0; i < headers.length; i++) {
                header.createCell(i).setCellValue(headers[i]);
            }
            ZoneId zone = ZoneId.systemDefault();
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(zone);
            int rowNum = 1;
            for (Interview i : list) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(i.getId() != null ? i.getId() : 0);
                row.createCell(1).setCellValue(i.getStartAt() != null ? fmt.format(i.getStartAt()) : "");
                row.createCell(2).setCellValue(i.getEndAt() != null ? fmt.format(i.getEndAt()) : "");
                row.createCell(3).setCellValue(i.getMode() != null ? i.getMode().name() : "");
                row.createCell(4).setCellValue(i.getStatus() != null ? i.getStatus().name() : "");
                row.createCell(5).setCellValue(i.getProjectId() != null ? i.getProjectId() : 0);
                row.createCell(6).setCellValue(i.getFreelancerId() != null ? i.getFreelancerId() : 0);
                row.createCell(7).setCellValue(i.getOwnerId() != null ? i.getOwnerId() : 0);
                row.createCell(8).setCellValue(trimToNull(i.getMeetingUrl()) != null ? i.getMeetingUrl() : "");
                row.createCell(9).setCellValue(trimToNull(i.getAddressLine()) != null ? i.getAddressLine() : "");
                row.createCell(10).setCellValue(trimToNull(i.getCity()) != null ? i.getCity() : "");
            }
            for (int c = 0; c < headers.length; c++) {
                sheet.autoSizeColumn(c);
            }
            wb.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to build Excel export", e);
        }
    }

    @Transactional
    public void delete(Long interviewId) {
        Interview interview = interviewRepository.findById(interviewId)
                .orElseThrow(() -> new RuntimeException("Interview not found with id: " + interviewId));

        if (interview.getSlotId() != null) {
            AvailabilitySlot slot = slotRepository.findById(interview.getSlotId()).orElse(null);
            if (slot != null) {
                slot.setBooked(false);
                slot.setBookedInterviewId(null);
                slotRepository.save(slot);
            }
        }

        interviewRepository.deleteById(interviewId);
    }

    private void validateMeetingFields(MeetingMode mode, String meetingUrl, String addressLine, String city) {
        if (mode == MeetingMode.FACE_TO_FACE) {
            if (trimToNull(addressLine) == null || trimToNull(city) == null) {
                throw new IllegalArgumentException("addressLine and city are required for FACE_TO_FACE interviews");
            }
        }
    }

    private void assertNoScheduleConflicts(Long freelancerId, Long ownerId, Instant startAt, Instant endAt, Long excludeId) {
        List<InterviewStatus> blocking = List.of(
                InterviewStatus.PROPOSED,
                InterviewStatus.CONFIRMED,
                InterviewStatus.COMPLETED
        );
        if (interviewRepository.existsOverlappingForFreelancer(freelancerId, blocking, startAt, endAt, excludeId)) {
            throw new IllegalStateException("Freelancer already has an interview overlapping this time range");
        }
        if (interviewRepository.existsOverlappingForOwner(ownerId, blocking, startAt, endAt, excludeId)) {
            throw new IllegalStateException("Owner already has an interview overlapping this time range");
        }
    }

    /**
     * Score a candidate suggestion: lower is better.
     * Base score is the absolute time distance in minutes from the preferred start.
     * Add a small penalty for very early or very late hours to favour office‑hour suggestions.
     */
    private long computeSuggestionScore(Instant preferredStart, Instant candidateStart) {
        long distanceMinutes = Math.abs(Duration.between(preferredStart, candidateStart).toMinutes());

        int hour = candidateStart.atZone(ZoneId.systemDefault()).getHour();
        long hourPenalty = 0;
        if (hour < 9) {
            hourPenalty = 30;
        } else if (hour > 18) {
            hourPenalty = 30;
        }

        return distanceMinutes + hourPenalty;
    }

    private boolean hasScheduleConflicts(Long freelancerId, Long ownerId, Instant startAt, Instant endAt, Long excludeId) {
        List<InterviewStatus> blocking = List.of(
                InterviewStatus.PROPOSED,
                InterviewStatus.CONFIRMED,
                InterviewStatus.COMPLETED
        );
        boolean freelancerBusy = interviewRepository.existsOverlappingForFreelancer(freelancerId, blocking, startAt, endAt, excludeId);
        boolean ownerBusy = interviewRepository.existsOverlappingForOwner(ownerId, blocking, startAt, endAt, excludeId);
        return freelancerBusy || ownerBusy;
    }

    private String trimToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private InterviewResponseDTO toDto(Interview i) {
        return new InterviewResponseDTO(
                i.getId(),
                i.getCandidatureId(),
                i.getProjectId(),
                i.getFreelancerId(),
                i.getOwnerId(),
                i.getSlotId(),
                i.getStartAt(),
                i.getEndAt(),
                i.getMode(),
                i.getMeetingUrl(),
                i.getAddressLine(),
                i.getCity(),
                i.getLat(),
                i.getLng(),
                i.getStatus(),
                i.getNotes(),
                i.getCreatedAt()
        );
    }
}

