package com.freelancing.productivity.service;

import com.freelancing.productivity.dto.GoalCreateRequestDTO;
import com.freelancing.productivity.dto.GoalResponseDTO;
import com.freelancing.productivity.entity.ProductivityGoal;
import com.freelancing.productivity.enums.ProductivityTaskStatus;
import com.freelancing.productivity.repository.ProductivityGoalRepository;
import com.freelancing.productivity.repository.ProductivityTaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GoalService {

    private final ProductivityGoalRepository goalRepository;
    private final ProductivityTaskRepository taskRepository;

    @Transactional(readOnly = true)
    public List<GoalResponseDTO> listByOwner(Long ownerId) {
        return goalRepository.findByOwnerIdOrderByCreatedAtDesc(ownerId)
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional
    public GoalResponseDTO create(Long ownerId, GoalCreateRequestDTO request) {
        ProductivityGoal goal = new ProductivityGoal();
        goal.setOwnerId(ownerId);
        goal.setTitle(request.getTitle().trim());
        goal.setDescription(request.getDescription());
        goal.setTargetDate(request.getTargetDate());
        return toDto(goalRepository.save(goal));
    }

    @Transactional
    public GoalResponseDTO update(Long goalId, GoalCreateRequestDTO request) {
        ProductivityGoal goal = find(goalId);
        goal.setTitle(request.getTitle().trim());
        goal.setDescription(request.getDescription());
        goal.setTargetDate(request.getTargetDate());
        return toDto(goalRepository.save(goal));
    }

    @Transactional
    public void delete(Long goalId) {
        if (!goalRepository.existsById(goalId)) {
            throw new IllegalArgumentException("Goal not found with id: " + goalId);
        }
        goalRepository.deleteById(goalId);
    }

    @Transactional(readOnly = true)
    public ProductivityGoal find(Long goalId) {
        return goalRepository.findById(goalId)
                .orElseThrow(() -> new IllegalArgumentException("Goal not found with id: " + goalId));
    }

    private GoalResponseDTO toDto(ProductivityGoal goal) {
        long total = taskRepository.countByOwnerIdAndGoalId(goal.getOwnerId(), goal.getId());
        long done = taskRepository.countByOwnerIdAndGoalIdAndStatus(goal.getOwnerId(), goal.getId(), ProductivityTaskStatus.DONE);

        // Weekly velocity: done tasks per week since goal creation.
        long days = Math.max(1, Duration.between(goal.getCreatedAt(), Instant.now()).toDays());
        double weeks = Math.max(1.0 / 7.0, (double) days / 7.0);

        GoalResponseDTO dto = new GoalResponseDTO();
        dto.setId(goal.getId());
        dto.setOwnerId(goal.getOwnerId());
        dto.setTitle(goal.getTitle());
        dto.setDescription(goal.getDescription());
        dto.setTargetDate(goal.getTargetDate());
        dto.setTotalTasks(total);
        dto.setDoneTasks(done);
        dto.setCompletionRate(total == 0 ? 0.0 : done / (double) total);
        dto.setWeeklyVelocity(done / weeks);
        return dto;
    }
}

