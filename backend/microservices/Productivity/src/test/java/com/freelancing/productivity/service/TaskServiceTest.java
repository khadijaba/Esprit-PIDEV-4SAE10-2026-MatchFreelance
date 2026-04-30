package com.freelancing.productivity.service;

import com.freelancing.productivity.dto.TaskCreateRequestDTO;
import com.freelancing.productivity.dto.TaskResponseDTO;
import com.freelancing.productivity.entity.ProductivityTask;
import com.freelancing.productivity.enums.ProductivityPriority;
import com.freelancing.productivity.enums.ProductivityTaskStatus;
import com.freelancing.productivity.repository.ProductivityTaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock
    private ProductivityTaskRepository taskRepository;

    @InjectMocks
    private TaskService taskService;

    private TaskCreateRequestDTO createRequest;

    @BeforeEach
    void setUp() {
        createRequest = new TaskCreateRequestDTO();
        createRequest.setTitle("  Implement API tests  ");
        createRequest.setDescription("Cover core task lifecycle");
        createRequest.setDueAt(Instant.parse("2026-04-20T12:00:00Z"));
    }

    @Test
    void createTask_shouldApplyDefaultsAndTrimTitle() {
        when(taskRepository.save(any(ProductivityTask.class))).thenAnswer(invocation -> {
            ProductivityTask task = invocation.getArgument(0);
            task.setId(10L);
            task.setCreatedAt(Instant.parse("2026-04-16T10:00:00Z"));
            task.setUpdatedAt(Instant.parse("2026-04-16T10:00:00Z"));
            task.setStatus(ProductivityTaskStatus.TODO);
            return task;
        });

        TaskResponseDTO result = taskService.createTask(5L, createRequest);

        assertEquals(10L, result.getId());
        assertEquals(5L, result.getOwnerId());
        assertEquals("Implement API tests", result.getTitle());
        assertEquals(ProductivityPriority.MEDIUM, result.getPriority());
        assertEquals(30, result.getPlannedMinutes());
        verify(taskRepository).save(any(ProductivityTask.class));
    }

    @Test
    void startTask_shouldThrowWhenTaskAlreadyDone() {
        ProductivityTask doneTask = new ProductivityTask();
        doneTask.setId(22L);
        doneTask.setStatus(ProductivityTaskStatus.DONE);

        when(taskRepository.findById(22L)).thenReturn(Optional.of(doneTask));

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> taskService.startTask(22L));

        assertEquals("Completed task cannot be restarted", ex.getMessage());
    }

    @Test
    void completeTask_shouldSetDoneStatusAndCompletionTime() {
        ProductivityTask task = new ProductivityTask();
        task.setId(33L);
        task.setOwnerId(5L);
        task.setTitle("Write tests");
        task.setStatus(ProductivityTaskStatus.TODO);
        task.setPriority(ProductivityPriority.HIGH);
        task.setPlannedMinutes(45);

        when(taskRepository.findById(33L)).thenReturn(Optional.of(task));
        when(taskRepository.save(any(ProductivityTask.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TaskResponseDTO result = taskService.completeTask(33L);

        assertEquals(ProductivityTaskStatus.DONE, result.getStatus());
        assertNotNull(result.getCompletedAt());
        verify(taskRepository).save(task);
    }
}

