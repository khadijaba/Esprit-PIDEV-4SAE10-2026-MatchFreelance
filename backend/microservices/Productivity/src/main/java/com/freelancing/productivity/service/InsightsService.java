package com.freelancing.productivity.service;

import com.freelancing.productivity.dto.ProductivityInsightsDTO;
import com.freelancing.productivity.dto.WeeklyReviewDTO;
import com.freelancing.productivity.entity.ProductivityTask;
import com.freelancing.productivity.enums.ProductivityTaskStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneId;
import java.util.*;

@Service
@RequiredArgsConstructor
public class InsightsService {

    private final TaskService taskService;

    @Transactional(readOnly = true)
    public ProductivityInsightsDTO insights(Long ownerId) {
        List<ProductivityTask> tasks = taskService.findAllTasks(ownerId);
        List<ProductivityTask> done = tasks.stream().filter(t -> t.getStatus() == ProductivityTaskStatus.DONE).toList();

        double completionRate = tasks.isEmpty() ? 0.0 : (double) done.size() / (double) tasks.size();
        double estimationAccuracy = estimationAccuracy(done);

        Map<Integer, Integer> donePerHour = new HashMap<>();
        int streak = 0;
        for (ProductivityTask task : done) {
            if (task.getCompletedAt() != null) {
                int hour = task.getCompletedAt().atZone(ZoneId.systemDefault()).getHour();
                donePerHour.put(hour, donePerHour.getOrDefault(hour, 0) + 1);
            }
            if (task.getCompletedAt() != null && task.getCreatedAt() != null) {
                streak++;
            }
        }

        int bestHour = donePerHour.entrySet().stream().max(Map.Entry.comparingByValue()).map(Map.Entry::getKey).orElse(10);
        int worstHour = donePerHour.entrySet().stream().min(Map.Entry.comparingByValue()).map(Map.Entry::getKey).orElse(15);

        ProductivityInsightsDTO dto = new ProductivityInsightsDTO();
        dto.setOwnerId(ownerId);
        dto.setCompletionRate(completionRate);
        dto.setEstimationAccuracyScore(estimationAccuracy);
        dto.setCurrentCompletionStreakDays(Math.min(streak, 30));
        dto.setBestPerformanceHour(bestHour);
        dto.setWorstPerformanceHour(worstHour);
        return dto;
    }

    @Transactional(readOnly = true)
    public WeeklyReviewDTO weeklyReview(Long ownerId) {
        WeeklyReviewDTO dto = new WeeklyReviewDTO();
        dto.setOwnerId(ownerId);
        dto.setPrompts(List.of(
                "What did you finish this week that moved a long-term goal forward?",
                "Which estimate was most off, and what signal did you miss?",
                "What dependency blocked progress and how can you remove it next week?",
                "Which task should you intentionally deprioritize and why?"
        ));
        return dto;
    }

    private double estimationAccuracy(List<ProductivityTask> doneTasks) {
        List<Double> scores = new ArrayList<>();
        for (ProductivityTask t : doneTasks) {
            if (t.getPlannedMinutes() == null || t.getPlannedMinutes() <= 0 || t.getActualMinutes() == null || t.getActualMinutes() <= 0) {
                continue;
            }
            double ratio = Math.abs((double) t.getActualMinutes() - (double) t.getPlannedMinutes()) / (double) t.getPlannedMinutes();
            double score = Math.max(0.0, 1.0 - ratio);
            scores.add(score);
        }

        if (scores.isEmpty()) {
            return 0.0;
        }

        double avg = scores.stream().reduce(0.0, Double::sum) / scores.size();
        return avg * 100.0;
    }
}

