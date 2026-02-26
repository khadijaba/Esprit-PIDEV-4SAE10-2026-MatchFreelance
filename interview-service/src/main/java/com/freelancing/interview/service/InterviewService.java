package com.freelancing.interview.service;

import com.freelancing.interview.dto.InterviewCreateRequestDTO;
import com.freelancing.interview.dto.InterviewResponseDTO;
import com.freelancing.interview.dto.InterviewUpdateRequestDTO;
import com.freelancing.interview.dto.VisioRoomResponseDTO;
import com.freelancing.interview.entity.AvailabilitySlot;
import com.freelancing.interview.entity.Interview;
import com.freelancing.interview.enums.InterviewStatus;
import com.freelancing.interview.enums.MeetingMode;
import com.freelancing.interview.repository.AvailabilitySlotRepository;
import com.freelancing.interview.enums.NotificationType;
import com.freelancing.interview.repository.InterviewRepository;
import com.freelancing.interview.repository.InterviewSpecifications;
import com.freelancing.interview.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class InterviewService {

    private static final DateTimeFormatter ICS_DATE_TIME_UTC =
            DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'").withZone(ZoneOffset.UTC);

    @Value("${interview.cancellation.allowed-hours-before:24}")
    private int cancellationAllowedHoursBefore;

    private final InterviewRepository interviewRepository;
    private final AvailabilitySlotRepository slotRepository;
    private final NotificationService notificationService;

    @Transactional
    public InterviewResponseDTO create(InterviewCreateRequestDTO req) {
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

        notificationService.emitToBoth(saved, NotificationType.INTERVIEW_PROPOSED);

        if (slotId != null) {
            AvailabilitySlot slot = slotRepository.findById(slotId).orElseThrow();
            slot.setBooked(true);
            slot.setBookedInterviewId(saved.getId());
            slotRepository.save(slot);
        }

        return toDto(saved);
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

    /** Mark interview as NO_SHOW (e.g. when cancellation is no longer allowed and candidate did not show). */
    @Transactional
    public InterviewResponseDTO noShow(Long interviewId) {
        Interview interview = interviewRepository.findById(interviewId)
                .orElseThrow(() -> new RuntimeException("Interview not found with id: " + interviewId));
        if (interview.getStatus() == InterviewStatus.CANCELLED) {
            throw new IllegalStateException("Cannot set NO_SHOW on a cancelled interview");
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

    @Transactional
    public InterviewResponseDTO complete(Long interviewId) {
        Interview interview = interviewRepository.findById(interviewId)
                .orElseThrow(() -> new RuntimeException("Interview not found with id: " + interviewId));
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
        String roomId = interview.getVisioRoomId();
        if (roomId == null || roomId.isBlank()) {
            roomId = "room-" + UUID.randomUUID();
            interview.setVisioRoomId(roomId);
            interviewRepository.save(interview);
        }
        // Always use in-app Jitsi room for embedded visio. meetingUrl remains as an external link if needed.
        String joinUrl = "https://meet.jit.si/freelance-interview-" + roomId;
        return new VisioRoomResponseDTO(roomId, joinUrl);
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

