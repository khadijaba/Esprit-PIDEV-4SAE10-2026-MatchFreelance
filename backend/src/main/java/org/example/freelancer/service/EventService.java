package org.example.freelancer.service;

import org.example.freelancer.entity.Event;
import org.example.freelancer.entity.EventStatus;
import org.example.freelancer.entity.EventType;
import org.example.freelancer.entity.Participation;
import org.example.freelancer.entity.ParticipationId;
import org.example.freelancer.entity.Reward;
import org.example.freelancer.repository.EventRepository;
import org.example.freelancer.repository.ParticipationRepository;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class EventService {

    private final EventRepository eventRepository;
    private final ParticipationRepository participationRepository;
    private final ChatClient chatClient;

    // Cooldown: block AI calls until this instant after a 429
    private volatile Instant rateLimitedUntil = Instant.MIN;

    // Parses "Please retry in 45.16s" from Google error messages
    private static final Pattern RETRY_AFTER_PATTERN =
            Pattern.compile("retry in (\\d+)\\.?\\d*s", Pattern.CASE_INSENSITIVE);

    public EventService(
            EventRepository eventRepository,
            ParticipationRepository participationRepository,
            ChatClient chatClient
    ) {
        this.eventRepository = eventRepository;
        this.participationRepository = participationRepository;
        this.chatClient = chatClient;
    }

    // ────────────────────────────────────────────
    // Event CRUD Operations
    // ────────────────────────────────────────────

    public Event create(Event event) {
        if (event.getPlannedRewards() == null) {
            event.setPlannedRewards(new ArrayList<>());
        }

        Event savedEvent = eventRepository.save(event);

        if (!savedEvent.getPlannedRewards().isEmpty()) {
            for (Reward reward : savedEvent.getPlannedRewards()) {
                reward.setEvent(savedEvent);
                reward.setRecipientId(null);
            }
            eventRepository.save(savedEvent);
        }

        return savedEvent;
    }

    public List<Event> getAll() {
        return eventRepository.findAll();
    }

    public Optional<Event> getById(Long id) {
        return eventRepository.findById(id);
    }

    public List<Event> getByStatus(EventStatus status) {
        return eventRepository.findByStatus(status);
    }

    public List<Event> getByType(EventType type) {
        return eventRepository.findByType(type);
    }

    public List<Event> getByStatusAndType(EventStatus status, EventType type) {
        return eventRepository.findByStatusAndType(status, type);
    }

    public List<Event> getUpcoming() {
        return eventRepository.findByStatusOrderByStartDateAsc(EventStatus.UPCOMING);
    }

    public Event update(Long id, Event updated) {
        return eventRepository.findById(id)
                .map(event -> {
                    event.setTitle(updated.getTitle());
                    event.setDescription(updated.getDescription());
                    event.setType(updated.getType());
                    event.setStartDate(updated.getStartDate());
                    event.setEndDate(updated.getEndDate());
                    event.setEligibilityCriteria(updated.getEligibilityCriteria());
                    event.setMaxParticipants(updated.getMaxParticipants());
                    event.setTeamEvent(updated.isTeamEvent());
                    event.setStatus(updated.getStatus());

                    if (updated.getPlannedRewards() != null) {
                        event.getPlannedRewards().clear();
                        updated.getPlannedRewards().forEach(reward -> {
                            reward.setEvent(event);
                            reward.setRecipientId(null);
                            event.getPlannedRewards().add(reward);
                        });
                    }

                    return eventRepository.save(event);
                })
                .orElseThrow(() -> new RuntimeException("Event not found with id " + id));
    }

    public void delete(Long id) {
        eventRepository.deleteById(id);
    }

    // ────────────────────────────────────────────
    // Participation Management
    // ────────────────────────────────────────────

    public Participation register(Long eventId, Long userId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Event not found with id " + eventId));

        if (event.getStatus() != EventStatus.UPCOMING && event.getStatus() != EventStatus.ONGOING) {
            throw new RuntimeException("Event is not open for registration");
        }

        if (event.getMaxParticipants() != null) {
            long currentCount = participationRepository.countByEventId(eventId);
            if (currentCount >= event.getMaxParticipants()) {
                throw new RuntimeException("Event has reached maximum capacity");
            }
        }

        ParticipationId pid = new ParticipationId(eventId, userId);
        if (participationRepository.existsById(pid)) {
            throw new RuntimeException("User is already registered for this event");
        }

        Participation participation = new Participation();
        participation.setEventId(eventId);
        participation.setUserId(userId);

        return participationRepository.save(participation);
    }

    public List<Participation> getParticipants(Long eventId) {
        return participationRepository.findByEventId(eventId);
    }

    public List<Participation> getLeaderboard(Long eventId) {
        return participationRepository.findLeaderboard(eventId);
    }

    public List<Participation> getUserParticipations(Long userId) {
        return participationRepository.findByUserId(userId);
    }

    // ────────────────────────────────────────────
    // AI-Powered Features
    // ────────────────────────────────────────────

    public String generateSummary(Long id) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Event not found with id " + id));

        if (isRateLimited()) {
            return rateLimitMessage();
        }

        String prompt = """
                Generate a concise 2-3 sentence summary of this event.
                Keep it brief, engaging, and professional.

                Title: %s
                Description: %s
                Type: %s
                Status: %s
                Eligibility: %s
                """.formatted(
                event.getTitle(),
                event.getDescription() != null ? event.getDescription() : "",
                event.getType(),
                event.getStatus(),
                event.getEligibilityCriteria() != null ? event.getEligibilityCriteria() : "Open to all"
        );

        return callAi(prompt, "Unable to generate summary at this time. Please try again later.");
    }

    public String answerQuestion(Long id, String question) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Event not found with id " + id));

        if (question == null || question.trim().isEmpty()) {
            return "Please provide a valid question.";
        }

        if (isRateLimited()) {
            return rateLimitMessage();
        }

        String prompt = """
                Answer this question about the event in 1-2 concise, helpful sentences.

                Question: %s

                Event Details:
                Title: %s
                Description: %s
                Type: %s
                Start: %s
                End: %s
                Max Participants: %s
                Team Event: %s
                Eligibility: %s
                """.formatted(
                question,
                event.getTitle(),
                event.getDescription() != null ? event.getDescription() : "",
                event.getType(),
                event.getStartDate() != null ? event.getStartDate() : "Not specified",
                event.getEndDate() != null ? event.getEndDate() : "Not specified",
                event.getMaxParticipants() != null ? event.getMaxParticipants() : "Unlimited",
                event.isTeamEvent() ? "Yes" : "No",
                event.getEligibilityCriteria() != null ? event.getEligibilityCriteria() : "Open to all"
        );

        return callAi(prompt, "Unable to process question at this time. Please try again later.");
    }

    public List<Event> searchByNaturalLanguage(String query) {
        if (query == null || query.trim().isEmpty()) {
            return getAll();
        }

        if (isRateLimited()) {
            return getAll();
        }

        List<Event> allEvents = getAll();

        String eventsList = allEvents.stream()
                .map(e -> e.getId() + "|" + e.getTitle() + "|" +
                        (e.getDescription() != null ? e.getDescription() : "") + "|" + e.getType())
                .collect(Collectors.joining("\n"));

        String prompt = """
                Given this search query: "%s"

                Here are all available events (format: ID|Title|Description|Type):
                %s

                Return ONLY a comma-separated list of event IDs that best match the query
                (most relevant first). No explanations, no extra text.
                If no events match, return exactly: NONE
                """.formatted(query, eventsList);

        try {
            String response = chatClient.prompt(prompt).call().content();

            if (response == null || response.contains("NONE") || response.trim().isEmpty()) {
                return List.of();
            }

            String[] idStrings = response.split(",");
            List<Event> result = new ArrayList<>();

            for (String idStr : idStrings) {
                try {
                    Long eid = Long.parseLong(idStr.trim());
                    allEvents.stream()
                            .filter(e -> e.getId() != null && e.getId().equals(eid))
                            .findFirst()
                            .ifPresent(result::add);
                } catch (NumberFormatException ignored) {
                    // Skip invalid ID
                }
            }

            return result;
        } catch (Exception e) {
            handleAiException(e);
            return allEvents;
        }
    }

    // ────────────────────────────────────────────
    // Private Helpers
    // ────────────────────────────────────────────

    private String callAi(String prompt, String fallback) {
        try {
            return chatClient.prompt(prompt).call().content();
        } catch (Exception e) {
            handleAiException(e);
            if (isRateLimited()) {
                return rateLimitMessage();
            }
            return fallback;
        }
    }

    /**
     * Detects 429 errors, parses the "retry in Xs" hint from Google response,
     * and sets the cooldown accordingly.
     */
    private void handleAiException(Exception e) {
        String msg = e.getMessage();
        if (msg == null || !msg.contains("429")) {
            return;
        }

        long secondsToWait = 60; // safe default

        // Try to extract exact retry-after from: "Please retry in 45.16s"
        Matcher matcher = RETRY_AFTER_PATTERN.matcher(msg);
        if (matcher.find()) {
            try {
                secondsToWait = Long.parseLong(matcher.group(1)) + 5; // +5s buffer
            } catch (NumberFormatException ignored) {
                // keep default
            }
        }

        rateLimitedUntil = Instant.now().plusSeconds(secondsToWait);
    }

    private boolean isRateLimited() {
        return Instant.now().isBefore(rateLimitedUntil);
    }

    private String rateLimitMessage() {
        long secondsLeft = rateLimitedUntil.getEpochSecond() - Instant.now().getEpochSecond();
        return String.format(
                "AI service is temporarily unavailable. Please try again in %d second(s).",
                Math.max(secondsLeft, 1)
        );
    }
}