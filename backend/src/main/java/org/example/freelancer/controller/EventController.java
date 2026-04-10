package org.example.freelancer.controller;

import org.example.freelancer.entity.Event;
import org.example.freelancer.entity.EventStatus;
import org.example.freelancer.entity.EventType;
import org.example.freelancer.entity.Participation;
import org.example.freelancer.entity.Reward;
import org.example.freelancer.service.EventService;
import org.example.freelancer.service.RewardService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@RequestMapping("/api/events")
@CrossOrigin(origins = "*")
public class EventController {

    private final EventService eventService;
    private final RewardService rewardService;

    public EventController(EventService eventService, RewardService rewardService) {
        this.eventService = eventService;
        this.rewardService = rewardService;
    }

    // ── Event CRUD ──

    @PostMapping
    public ResponseEntity<Event> create(@RequestBody Event event) {
        Event created = eventService.create(event);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping
    public ResponseEntity<List<Event>> getAll(
            @RequestParam(required = false) EventStatus status,
            @RequestParam(required = false) EventType type) {
        List<Event> events;
        if (status != null && type != null) {
            events = eventService.getByStatusAndType(status, type);
        } else if (status != null) {
            events = eventService.getByStatus(status);
        } else if (type != null) {
            events = eventService.getByType(type);
        } else {
            events = eventService.getAll();
        }
        return ResponseEntity.ok(events);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Event> getById(@PathVariable Long id) {
        return eventService.getById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<Event> update(@PathVariable Long id, @RequestBody Event event) {
        try {
            Event updated = eventService.update(id, event);
            return ResponseEntity.ok(updated);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        eventService.delete(id);
        return ResponseEntity.noContent().build();
    }

    // ── Participation ──

    @PostMapping("/{id}/register")
    public ResponseEntity<?> register(@PathVariable Long id, @RequestParam Long userId) {
        try {
            Participation participation = eventService.register(id, userId);
            return ResponseEntity.status(HttpStatus.CREATED).body(participation);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/{id}/participants")
    public ResponseEntity<List<Participation>> getParticipants(@PathVariable Long id) {
        return ResponseEntity.ok(eventService.getParticipants(id));
    }

    @GetMapping("/{id}/leaderboard")
    public ResponseEntity<List<Participation>> getLeaderboard(@PathVariable Long id) {
        return ResponseEntity.ok(eventService.getLeaderboard(id));
    }

    // ── Rewards ──

    @PostMapping("/{id}/award")
    public ResponseEntity<?> awardReward(@PathVariable Long id, @RequestBody Reward reward) {
        try {
            Event event = eventService.getById(id)
                    .orElseThrow(() -> new RuntimeException("Event not found"));
            reward.setEvent(event);
            Reward created = rewardService.award(reward);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/{id}/rewards")
    public ResponseEntity<List<Reward>> getEventRewards(@PathVariable Long id) {
        return ResponseEntity.ok(rewardService.getByEvent(id));
    }

    @GetMapping("/users/{userId}/rewards")
    public ResponseEntity<List<Reward>> getUserRewards(@PathVariable Long userId) {
        return ResponseEntity.ok(rewardService.getByRecipient(userId));
    }

    @PutMapping("/rewards/{rewardId}/revoke")
    public ResponseEntity<?> revokeReward(@PathVariable Long rewardId) {
        try {
            Reward revoked = rewardService.revoke(rewardId);
            return ResponseEntity.ok(revoked);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // ── AI Features ──

    @GetMapping("/{id}/summary")
    public ResponseEntity<String> generateSummary(@PathVariable Long id) {
        try {
            String summary = eventService.generateSummary(id);
            return ResponseEntity.ok(summary);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/{id}/qa")
    public ResponseEntity<String> answerQuestion(@PathVariable Long id, @RequestParam String question) {
        try {
            String answer = eventService.answerQuestion(id, question);
            return ResponseEntity.ok(answer);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/search")
    public ResponseEntity<List<Event>> searchByNaturalLanguage(@RequestParam String query) {
        List<Event> results = eventService.searchByNaturalLanguage(query);
        return ResponseEntity.ok(results);
    }
}
