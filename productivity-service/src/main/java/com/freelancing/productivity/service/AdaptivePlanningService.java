package com.freelancing.productivity.service;

import com.freelancing.productivity.dto.AdaptiveRescheduleRequestDTO;
import com.freelancing.productivity.dto.AdaptiveRescheduleResponseDTO;
import com.freelancing.productivity.dto.DailyTaskAllocationDTO;
import com.freelancing.productivity.dto.TaskOrderResponseDTO;
import com.freelancing.productivity.entity.ProductivityTask;
import com.freelancing.productivity.enums.ProductivityTaskStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;

@Service
@RequiredArgsConstructor
public class AdaptivePlanningService {

    private final TaskService taskService;
    private final DependencyService dependencyService;

    @Transactional(readOnly = true)
    public AdaptiveRescheduleResponseDTO buildAdaptiveWeekPlan(Long ownerId, AdaptiveRescheduleRequestDTO request) {
        LocalDate weekStart = request.getWeekStart() == null ? LocalDate.now() : request.getWeekStart();
        int dailyCapacity = request.getDailyCapacityMinutes() == null ? 240 : Math.max(60, Math.min(request.getDailyCapacityMinutes(), 720));

        List<ProductivityTask> active = taskService.findAllTasks(ownerId).stream()
                .filter(t -> t.getStatus() != ProductivityTaskStatus.DONE)
                .toList();

        Map<Long, ProductivityTask> byId = new HashMap<>();
        for (ProductivityTask task : active) {
            byId.put(task.getId(), task);
        }

        List<ProductivityTask> ordered = new ArrayList<>();
        try {
            TaskOrderResponseDTO order = dependencyService.topologicalOrder(ownerId);
            for (Long id : order.getOrderedTaskIds()) {
                ProductivityTask task = byId.get(id);
                if (task != null) {
                    ordered.add(task);
                }
            }
            // Include any remaining active tasks not captured in graph traversal.
            for (ProductivityTask task : active) {
                if (!ordered.contains(task)) {
                    ordered.add(task);
                }
            }
        } catch (Exception ignored) {
            ordered.addAll(active);
        }

        ordered.sort(Comparator
                .comparing((ProductivityTask t) -> t.getDueAt() == null ? LocalDate.MAX : t.getDueAt().atZone(ZoneId.systemDefault()).toLocalDate())
                .thenComparing(ProductivityTask::getPriority)
                .reversed());

        List<DailyTaskAllocationDTO> allocations = new ArrayList<>();
        LocalDate cursor = weekStart;
        int remainingCapacity = dailyCapacity;

        for (ProductivityTask task : ordered) {
            int remainingMinutes = Math.max(15, (task.getPlannedMinutes() == null ? 30 : task.getPlannedMinutes())
                    - (task.getActualMinutes() == null ? 0 : task.getActualMinutes()));

            while (remainingMinutes > 0) {
                if (remainingCapacity <= 0) {
                    cursor = cursor.plusDays(1);
                    remainingCapacity = dailyCapacity;
                }

                int chunk = Math.min(remainingMinutes, remainingCapacity);
                DailyTaskAllocationDTO slot = new DailyTaskAllocationDTO();
                slot.setTaskId(task.getId());
                slot.setTitle(task.getTitle());
                slot.setScheduledDate(cursor);
                slot.setAllocatedMinutes(chunk);
                allocations.add(slot);

                remainingMinutes -= chunk;
                remainingCapacity -= chunk;
            }
        }

        AdaptiveRescheduleResponseDTO response = new AdaptiveRescheduleResponseDTO();
        response.setOwnerId(ownerId);
        response.setWeekStart(weekStart);
        response.setDailyCapacityMinutes(dailyCapacity);
        response.setAllocations(allocations);
        response.setAiSource("HEURISTIC");
        return response;
    }
}

