package com.freelancing.productivity.service;

import com.freelancing.productivity.dto.ProgressSummaryDTO;
import com.freelancing.productivity.entity.ProductivityTask;
import com.freelancing.productivity.enums.ProductivityTaskStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProgressService {

    private final TaskService taskService;

    @Transactional(readOnly = true)
    public ProgressSummaryDTO summarize(Long ownerId) {
        List<ProductivityTask> tasks = taskService.findAllTasks(ownerId);
        long total = tasks.size();
        long done = tasks.stream().filter(t -> t.getStatus() == ProductivityTaskStatus.DONE).count();
        long inProgress = tasks.stream().filter(t -> t.getStatus() == ProductivityTaskStatus.IN_PROGRESS).count();
        long blocked = tasks.stream().filter(t -> t.getStatus() == ProductivityTaskStatus.BLOCKED).count();

        Instant now = Instant.now();
        long overdue = tasks.stream()
                .filter(t -> t.getStatus() != ProductivityTaskStatus.DONE)
                .filter(t -> t.getDueAt() != null && t.getDueAt().isBefore(now))
                .count();

        int openMinutes = tasks.stream()
                .filter(t -> t.getStatus() != ProductivityTaskStatus.DONE)
                .map(ProductivityTask::getPlannedMinutes)
                .filter(v -> v != null && v > 0)
                .reduce(0, Integer::sum);

        ProgressSummaryDTO dto = new ProgressSummaryDTO();
        dto.setOwnerId(ownerId);
        dto.setTotalTasks(total);
        dto.setDoneTasks(done);
        dto.setInProgressTasks(inProgress);
        dto.setBlockedTasks(blocked);
        dto.setOverdueTasks(overdue);
        dto.setCompletionRate(total == 0 ? 0.0 : ((double) done / (double) total));
        dto.setTotalPlannedMinutesOpen(openMinutes);
        return dto;
    }
}

