package com.freelancing.productivity.service;

import com.freelancing.productivity.dto.PageResponseDTO;
import com.freelancing.productivity.dto.TaskCreateRequestDTO;
import com.freelancing.productivity.dto.TaskResponseDTO;
import com.freelancing.productivity.dto.TaskUpdateRequestDTO;
import com.freelancing.productivity.entity.ProductivityTask;
import com.freelancing.productivity.enums.ProductivityPriority;
import com.freelancing.productivity.enums.ProductivityTaskStatus;
import com.freelancing.productivity.repository.ProductivityTaskRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class TaskService {

    private static final Logger logger = LoggerFactory.getLogger(TaskService.class);

    private final ProductivityTaskRepository taskRepository;

    @Transactional(readOnly = true)
    public List<TaskResponseDTO> getTasksByOwner(Long ownerId) {
        return taskRepository.findByOwnerIdOrderByDueAtAscCreatedAtDesc(ownerId)
                .stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public PageResponseDTO<TaskResponseDTO> getTasksByOwnerPaged(
            Long ownerId,
            String query,
            ProductivityTaskStatus status,
            ProductivityPriority priority,
            Instant dueFrom,
            Instant dueTo,
            int page,
            int size
    ) {
        int safePage = Math.max(0, page);
        int safeSize = Math.max(1, Math.min(size, 50));
        PageRequest pageRequest = PageRequest.of(
                safePage,
                safeSize,
                Sort.by(Sort.Order.asc("dueAt"), Sort.Order.desc("createdAt"))
        );

        Specification<ProductivityTask> spec = (root, q, cb) -> cb.equal(root.get("ownerId"), ownerId);

        if (query != null && !query.isBlank()) {
            String normalized = "%" + query.trim().toLowerCase(Locale.ROOT) + "%";
            spec = spec.and((root, q, cb) -> cb.or(
                    cb.like(cb.lower(root.get("title")), normalized),
                    cb.like(cb.lower(root.get("description")), normalized)
            ));
        }

        if (status != null) {
            spec = spec.and((root, q, cb) -> cb.equal(root.get("status"), status));
        }

        if (priority != null) {
            spec = spec.and((root, q, cb) -> cb.equal(root.get("priority"), priority));
        }

        if (dueFrom != null) {
            spec = spec.and((root, q, cb) -> cb.greaterThanOrEqualTo(root.get("dueAt"), dueFrom));
        }

        if (dueTo != null) {
            spec = spec.and((root, q, cb) -> cb.lessThanOrEqualTo(root.get("dueAt"), dueTo));
        }

        Page<TaskResponseDTO> mapped = taskRepository.findAll(spec, pageRequest).map(this::toDto);
        return PageResponseDTO.from(mapped);
    }

    @Transactional
    public TaskResponseDTO createTask(Long ownerId, TaskCreateRequestDTO request) {
        ProductivityTask task = new ProductivityTask();
        task.setOwnerId(ownerId);
        task.setGoalId(request.getGoalId());
        task.setTitle(request.getTitle().trim());
        task.setDescription(request.getDescription());
        task.setPriority(request.getPriority() == null ? ProductivityPriority.MEDIUM : request.getPriority());
        task.setPlannedMinutes(request.getPlannedMinutes() == null ? 30 : request.getPlannedMinutes());
        task.setActualMinutes(request.getActualMinutes());
        task.setDueAt(request.getDueAt());
        ProductivityTask saved = taskRepository.save(task);
        logger.info("Created task {} for owner {}", saved.getId(), ownerId);
        return toDto(saved);
    }

    @Transactional
    public TaskResponseDTO updateTask(Long taskId, TaskUpdateRequestDTO request) {
        ProductivityTask task = findTask(taskId);
        if (request.getTitle() != null && !request.getTitle().isBlank()) {
            task.setTitle(request.getTitle().trim());
        }
        if (request.getDescription() != null) {
            task.setDescription(request.getDescription());
        }
        if (request.getPriority() != null) {
            task.setPriority(request.getPriority());
        }
        if (request.getPlannedMinutes() != null) {
            task.setPlannedMinutes(request.getPlannedMinutes());
        }
        if (request.getActualMinutes() != null) {
            task.setActualMinutes(request.getActualMinutes());
        }
        if (request.getGoalId() != null) {
            task.setGoalId(request.getGoalId());
        }
        if (request.getDueAt() != null) {
            task.setDueAt(request.getDueAt());
        }
        if (request.getStatus() != null) {
            applyStatusTransition(task, request.getStatus());
        }
        return toDto(taskRepository.save(task));
    }

    @Transactional
    public TaskResponseDTO clearTaskGoal(Long taskId) {
        ProductivityTask task = findTask(taskId);
        task.setGoalId(null);
        return toDto(taskRepository.save(task));
    }

    @Transactional
    public TaskResponseDTO startTask(Long taskId) {
        ProductivityTask task = findTask(taskId);
        if (task.getStatus() == ProductivityTaskStatus.DONE) {
            throw new IllegalStateException("Completed task cannot be restarted");
        }
        task.setStatus(ProductivityTaskStatus.IN_PROGRESS);
        return toDto(taskRepository.save(task));
    }

    @Transactional
    public TaskResponseDTO completeTask(Long taskId) {
        ProductivityTask task = findTask(taskId);
        task.setStatus(ProductivityTaskStatus.DONE);
        task.setCompletedAt(Instant.now());
        return toDto(taskRepository.save(task));
    }

    @Transactional
    public void deleteTask(Long taskId) {
        if (!taskRepository.existsById(taskId)) {
            throw new IllegalArgumentException("Task not found with id: " + taskId);
        }
        taskRepository.deleteById(taskId);
    }

    @Transactional(readOnly = true)
    public String exportOwnerCalendar(Long ownerId) {
        List<ProductivityTask> tasks = taskRepository.findByOwnerIdOrderByDueAtAscCreatedAtDesc(ownerId);
        return IcsUtils.tasksToIcs("Productivity Tasks", tasks);
    }

    @Transactional(readOnly = true)
    public String exportSingleTaskCalendar(Long taskId) {
        ProductivityTask task = findTask(taskId);
        return IcsUtils.tasksToIcs("Task " + task.getId(), List.of(task));
    }

    @Transactional(readOnly = true)
    public List<ProductivityTask> findActiveTasks(Long ownerId) {
        return taskRepository.findByOwnerIdAndStatusInOrderByDueAtAscCreatedAtDesc(ownerId,
                List.of(ProductivityTaskStatus.TODO, ProductivityTaskStatus.IN_PROGRESS, ProductivityTaskStatus.BLOCKED));
    }

    @Transactional(readOnly = true)
    public List<ProductivityTask> findAllTasks(Long ownerId) {
        return taskRepository.findByOwnerIdOrderByDueAtAscCreatedAtDesc(ownerId);
    }

    @Transactional(readOnly = true)
    public ProductivityTask findTaskEntity(Long taskId) {
        return findTask(taskId);
    }

    private void applyStatusTransition(ProductivityTask task, ProductivityTaskStatus targetStatus) {
        if (targetStatus == ProductivityTaskStatus.DONE) {
            task.setCompletedAt(Instant.now());
        }
        if (targetStatus != ProductivityTaskStatus.DONE) {
            task.setCompletedAt(null);
        }
        task.setStatus(targetStatus);
    }

    private ProductivityTask findTask(Long taskId) {
        return taskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Task not found with id: " + taskId));
    }

    private TaskResponseDTO toDto(ProductivityTask task) {
        TaskResponseDTO dto = new TaskResponseDTO();
        dto.setId(task.getId());
        dto.setOwnerId(task.getOwnerId());
        dto.setGoalId(task.getGoalId());
        dto.setTitle(task.getTitle());
        dto.setDescription(task.getDescription());
        dto.setStatus(task.getStatus());
        dto.setPriority(task.getPriority());
        dto.setPlannedMinutes(task.getPlannedMinutes());
        dto.setActualMinutes(task.getActualMinutes());
        dto.setDueAt(task.getDueAt());
        dto.setCompletedAt(task.getCompletedAt());
        dto.setCreatedAt(task.getCreatedAt());
        dto.setUpdatedAt(task.getUpdatedAt());
        return dto;
    }
}

