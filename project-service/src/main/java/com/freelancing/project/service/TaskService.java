package com.freelancing.project.service;

import com.freelancing.project.dto.TaskRequestDTO;
import com.freelancing.project.dto.TaskResponseDTO;
import com.freelancing.project.entity.Task;
import com.freelancing.project.enums.TaskStatus;
import com.freelancing.project.repository.ProjectRepository;
import com.freelancing.project.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;
    private final ProjectRepository projectRepository;

    @Transactional(readOnly = true)
    public List<TaskResponseDTO> getTasksByProjectId(Long projectId) {
        if (!projectRepository.existsById(projectId)) {
            throw new RuntimeException("Project not found with id: " + projectId);
        }
        return taskRepository.findByProjectId(projectId).stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public TaskResponseDTO createTask(Long projectId, TaskRequestDTO requestDTO) {
        if (!projectRepository.existsById(projectId)) {
            throw new RuntimeException("Project not found with id: " + projectId);
        }
        Task task = new Task();
        task.setProjectId(projectId);
        task.setTitle(requestDTO.getTitle());
        task.setDescription(requestDTO.getDescription());
        task.setStatus(requestDTO.getStatus() != null ? requestDTO.getStatus() : TaskStatus.TODO);
        task.setDueDate(requestDTO.getDueDate());
        task.setAssigneeId(requestDTO.getAssigneeId());
        Task saved = taskRepository.save(task);
        return toResponseDTO(saved);
    }

    @Transactional
    public TaskResponseDTO updateTask(Long projectId, Long taskId, TaskRequestDTO requestDTO) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found with id: " + taskId));
        if (!task.getProjectId().equals(projectId)) {
            throw new RuntimeException("Task does not belong to project " + projectId);
        }
        if (requestDTO.getTitle() != null) task.setTitle(requestDTO.getTitle());
        if (requestDTO.getDescription() != null) task.setDescription(requestDTO.getDescription());
        if (requestDTO.getStatus() != null) task.setStatus(requestDTO.getStatus());
        if (requestDTO.getDueDate() != null) task.setDueDate(requestDTO.getDueDate());
        if (requestDTO.getAssigneeId() != null) task.setAssigneeId(requestDTO.getAssigneeId());
        return toResponseDTO(taskRepository.save(task));
    }

    @Transactional
    public void deleteTask(Long projectId, Long taskId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found with id: " + taskId));
        if (!task.getProjectId().equals(projectId)) {
            throw new RuntimeException("Task does not belong to project " + projectId);
        }
        taskRepository.deleteById(taskId);
    }

    private TaskResponseDTO toResponseDTO(Task t) {
        TaskResponseDTO dto = new TaskResponseDTO();
        dto.setId(t.getId());
        dto.setProjectId(t.getProjectId());
        dto.setTitle(t.getTitle());
        dto.setDescription(t.getDescription());
        dto.setStatus(t.getStatus());
        dto.setDueDate(t.getDueDate());
        dto.setAssigneeId(t.getAssigneeId());
        return dto;
    }
}
