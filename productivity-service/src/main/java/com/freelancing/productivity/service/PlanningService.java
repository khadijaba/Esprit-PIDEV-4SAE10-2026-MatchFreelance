package com.freelancing.productivity.service;

import com.freelancing.productivity.dto.DailyPlanDTO;
import com.freelancing.productivity.dto.PlanningSuggestionDTO;
import com.freelancing.productivity.entity.ProductivityTask;
import com.freelancing.productivity.enums.ProductivityPriority;
import com.freelancing.productivity.enums.ProductivityTaskStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PlanningService {

    private final TaskService taskService;

    @Transactional(readOnly = true)
    public List<PlanningSuggestionDTO> suggest(Long ownerId, int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 20));
        Instant now = Instant.now();

        return taskService.findActiveTasks(ownerId)
                .stream()
                .filter(task -> task.getStatus() != ProductivityTaskStatus.DONE)
                .map(task -> toSuggestion(task, now))
                .sorted(Comparator.comparingDouble(PlanningSuggestionDTO::getScore).reversed())
                .limit(safeLimit)
                .toList();
    }

    @Transactional(readOnly = true)
    public DailyPlanDTO buildDailyPlan(Long ownerId, int focusMinutes) {
        int safeFocus = Math.max(30, Math.min(focusMinutes, 720));

        // Pull enough candidates for optimization while keeping CPU/memory bounded.
        List<PlanningSuggestionDTO> candidates = suggest(ownerId, 20);
        int n = candidates.size();

        List<PlanningSuggestionDTO> picked;
        if (n == 0) {
            picked = new ArrayList<>();
        } else {
            int[] weights = new int[n];
            int[] values = new int[n];

            for (int i = 0; i < n; i++) {
                PlanningSuggestionDTO s = candidates.get(i);
                int minutes = s.getPlannedMinutes() == null ? 30 : s.getPlannedMinutes();
                weights[i] = Math.max(5, Math.min(minutes, 720));
                values[i] = (int) Math.round(s.getScore() * 100.0);
            }

            int[][] dp = new int[n + 1][safeFocus + 1];
            boolean[][] take = new boolean[n + 1][safeFocus + 1];

            for (int i = 1; i <= n; i++) {
                int w = weights[i - 1];
                int v = values[i - 1];
                for (int cap = 0; cap <= safeFocus; cap++) {
                    int skipScore = dp[i - 1][cap];
                    int takeScore = Integer.MIN_VALUE;
                    if (w <= cap) {
                        takeScore = dp[i - 1][cap - w] + v;
                    }
                    if (takeScore > skipScore) {
                        dp[i][cap] = takeScore;
                        take[i][cap] = true;
                    } else {
                        dp[i][cap] = skipScore;
                    }
                }
            }

            picked = new ArrayList<>();
            int cap = safeFocus;
            for (int i = n; i >= 1; i--) {
                if (take[i][cap]) {
                    picked.add(candidates.get(i - 1));
                    cap -= weights[i - 1];
                }
            }

            // Stable presentation: highest score first.
            picked.sort(Comparator.comparingDouble(PlanningSuggestionDTO::getScore).reversed());

            // Fallback: if nothing fits, still suggest the best single task.
            if (picked.isEmpty() && !candidates.isEmpty()) {
                picked.add(candidates.get(0));
            }
        }

        int allocated = picked.stream()
                .map(PlanningSuggestionDTO::getPlannedMinutes)
                .map(v -> v == null ? 30 : v)
                .reduce(0, Integer::sum);

        double totalScore = picked.stream().mapToDouble(PlanningSuggestionDTO::getScore).sum();

        DailyPlanDTO dto = new DailyPlanDTO();
        dto.setOwnerId(ownerId);
        dto.setFocusMinutes(safeFocus);
        dto.setAllocatedMinutes(allocated);
        dto.setUtilizationRate(safeFocus == 0 ? 0.0 : (double) allocated / (double) safeFocus);
        dto.setAllocationScore(totalScore);
        dto.setAlgorithmUsed("KNAPSACK_01");
        dto.setTasks(picked);
        return dto;
    }

    private PlanningSuggestionDTO toSuggestion(ProductivityTask task, Instant now) {
        double score = 0.0;
        StringBuilder rationale = new StringBuilder();

        score += priorityWeight(task.getPriority());
        rationale.append("Priority=").append(task.getPriority());

        if (task.getStatus() == ProductivityTaskStatus.IN_PROGRESS) {
            score += 15;
            rationale.append("; already started");
        }

        if (task.getDueAt() != null) {
            long hours = Duration.between(now, task.getDueAt()).toHours();
            if (task.getDueAt().isBefore(now)) {
                score += 90;
                rationale.append("; overdue");
            } else if (hours <= 24) {
                score += 70;
                rationale.append("; due in <24h");
            } else if (hours <= 72) {
                score += 40;
                rationale.append("; due in <72h");
            } else {
                score += 10;
                rationale.append("; due later");
            }
        }

        if (task.getPlannedMinutes() != null && task.getPlannedMinutes() <= 30) {
            score += 8;
            rationale.append("; quick win");
        }

        PlanningSuggestionDTO dto = new PlanningSuggestionDTO();
        dto.setTaskId(task.getId());
        dto.setTitle(task.getTitle());
        dto.setPriority(task.getPriority().name());
        dto.setDueAt(task.getDueAt());
        dto.setPlannedMinutes(task.getPlannedMinutes());
        dto.setScore(score);
        dto.setRationale(rationale.toString());
        return dto;
    }

    private double priorityWeight(ProductivityPriority priority) {
        return switch (priority) {
            case LOW -> 10;
            case MEDIUM -> 20;
            case HIGH -> 35;
            case URGENT -> 55;
        };
    }
}

