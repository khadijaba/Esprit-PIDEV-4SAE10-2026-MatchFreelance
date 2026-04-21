package com.freelancing.productivity.service;

import com.freelancing.productivity.dto.ConflictResolutionRequestDTO;
import com.freelancing.productivity.dto.ConflictResolutionResponseDTO;
import com.freelancing.productivity.entity.ProductivityTask;
import com.freelancing.productivity.enums.ProductivityPriority;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class DecisionIntelligenceService {

    private final TaskService taskService;

    @Transactional(readOnly = true)
    public ConflictResolutionResponseDTO resolve(ConflictResolutionRequestDTO request) {
        ProductivityTask first = taskService.findTaskEntity(request.getFirstTaskId());
        ProductivityTask second = taskService.findTaskEntity(request.getSecondTaskId());

        double firstScore = score(first);
        double secondScore = score(second);

        ProductivityTask recommended = firstScore >= secondScore ? first : second;
        ProductivityTask deferred = firstScore >= secondScore ? second : first;

        ConflictResolutionResponseDTO response = new ConflictResolutionResponseDTO();
        response.setRecommendedTaskId(recommended.getId());
        response.setDeferredTaskId(deferred.getId());
        response.setRecommendedScore(Math.max(firstScore, secondScore));
        response.setDeferredScore(Math.min(firstScore, secondScore));
        response.setRationale("Recommended task has higher weighted urgency and strategic value (priority + due pressure + progress state).");
        response.setAiSource("HEURISTIC");
        return response;
    }

    private double score(ProductivityTask task) {
        double score = priorityWeight(task.getPriority());

        if (task.getDueAt() != null) {
            long hours = Duration.between(Instant.now(), task.getDueAt()).toHours();
            if (task.getDueAt().isBefore(Instant.now())) {
                score += 100;
            } else if (hours <= 24) {
                score += 65;
            } else if (hours <= 72) {
                score += 35;
            } else {
                score += 10;
            }
        }

        if (task.getStatus() != null && task.getStatus().name().equals("IN_PROGRESS")) {
            score += 12;
        }

        int planned = task.getPlannedMinutes() == null ? 30 : task.getPlannedMinutes();
        if (planned <= 45) {
            score += 6;
        }
        return score;
    }

    private double priorityWeight(ProductivityPriority priority) {
        if (priority == null) return 20;
        return switch (priority) {
            case LOW -> 10;
            case MEDIUM -> 20;
            case HIGH -> 35;
            case URGENT -> 55;
        };
    }
}

