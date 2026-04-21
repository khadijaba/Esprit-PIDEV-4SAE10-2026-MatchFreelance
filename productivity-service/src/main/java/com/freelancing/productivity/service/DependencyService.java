package com.freelancing.productivity.service;

import com.freelancing.productivity.dto.DependencyCreateRequestDTO;
import com.freelancing.productivity.dto.DependencyResponseDTO;
import com.freelancing.productivity.dto.TaskOrderResponseDTO;
import com.freelancing.productivity.entity.ProductivityTask;
import com.freelancing.productivity.entity.TaskDependency;
import com.freelancing.productivity.repository.TaskDependencyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
public class DependencyService {

    private final TaskDependencyRepository dependencyRepository;
    private final TaskService taskService;

    @Transactional(readOnly = true)
    public List<DependencyResponseDTO> listByOwner(Long ownerId) {
        return dependencyRepository.findByOwnerId(ownerId).stream().map(this::toDto).toList();
    }

    @Transactional
    public DependencyResponseDTO addDependency(Long ownerId, DependencyCreateRequestDTO request) {
        if (Objects.equals(request.getPredecessorTaskId(), request.getSuccessorTaskId())) {
            throw new IllegalArgumentException("A task cannot depend on itself");
        }

        ProductivityTask predecessor = taskService.findTaskEntity(request.getPredecessorTaskId());
        ProductivityTask successor = taskService.findTaskEntity(request.getSuccessorTaskId());
        if (!Objects.equals(predecessor.getOwnerId(), ownerId) || !Objects.equals(successor.getOwnerId(), ownerId)) {
            throw new IllegalArgumentException("Dependencies must reference tasks owned by the same user");
        }

        if (dependencyRepository.existsByOwnerIdAndPredecessorTaskIdAndSuccessorTaskId(
                ownerId,
                request.getPredecessorTaskId(),
                request.getSuccessorTaskId())) {
            throw new IllegalArgumentException("Dependency already exists");
        }

        List<TaskDependency> current = dependencyRepository.findByOwnerId(ownerId);
        if (createsCycle(current, request.getPredecessorTaskId(), request.getSuccessorTaskId())) {
            throw new IllegalStateException("Dependency creates a cycle in the task graph");
        }

        TaskDependency dependency = new TaskDependency();
        dependency.setOwnerId(ownerId);
        dependency.setPredecessorTaskId(request.getPredecessorTaskId());
        dependency.setSuccessorTaskId(request.getSuccessorTaskId());
        return toDto(dependencyRepository.save(dependency));
    }

    @Transactional
    public void removeDependency(Long dependencyId) {
        if (!dependencyRepository.existsById(dependencyId)) {
            throw new IllegalArgumentException("Dependency not found with id: " + dependencyId);
        }
        dependencyRepository.deleteById(dependencyId);
    }

    @Transactional(readOnly = true)
    public TaskOrderResponseDTO topologicalOrder(Long ownerId) {
        List<ProductivityTask> tasks = taskService.findAllTasks(ownerId);
        List<TaskDependency> dependencies = dependencyRepository.findByOwnerId(ownerId);

        Map<Long, Integer> indegree = new HashMap<>();
        Map<Long, List<Long>> graph = new HashMap<>();

        for (ProductivityTask task : tasks) {
            indegree.put(task.getId(), 0);
            graph.put(task.getId(), new ArrayList<>());
        }

        for (TaskDependency d : dependencies) {
            if (!graph.containsKey(d.getPredecessorTaskId()) || !graph.containsKey(d.getSuccessorTaskId())) {
                continue;
            }
            graph.get(d.getPredecessorTaskId()).add(d.getSuccessorTaskId());
            indegree.put(d.getSuccessorTaskId(), indegree.getOrDefault(d.getSuccessorTaskId(), 0) + 1);
        }

        Deque<Long> queue = new ArrayDeque<>();
        for (Map.Entry<Long, Integer> e : indegree.entrySet()) {
            if (e.getValue() == 0) {
                queue.add(e.getKey());
            }
        }

        List<Long> ordered = new ArrayList<>();
        while (!queue.isEmpty()) {
            Long current = queue.removeFirst();
            ordered.add(current);
            for (Long next : graph.getOrDefault(current, List.of())) {
                int newDegree = indegree.get(next) - 1;
                indegree.put(next, newDegree);
                if (newDegree == 0) {
                    queue.add(next);
                }
            }
        }

        if (ordered.size() != tasks.size()) {
            throw new IllegalStateException("Task graph contains a cycle");
        }

        TaskOrderResponseDTO response = new TaskOrderResponseDTO();
        response.setOwnerId(ownerId);
        response.setOrderedTaskIds(ordered);
        return response;
    }

    private boolean createsCycle(List<TaskDependency> dependencies, Long predecessor, Long successor) {
        Map<Long, List<Long>> graph = new HashMap<>();
        for (TaskDependency d : dependencies) {
            graph.computeIfAbsent(d.getPredecessorTaskId(), k -> new ArrayList<>()).add(d.getSuccessorTaskId());
        }
        graph.computeIfAbsent(predecessor, k -> new ArrayList<>()).add(successor);

        Set<Long> visiting = new HashSet<>();
        Set<Long> visited = new HashSet<>();
        for (Long node : graph.keySet()) {
            if (hasCycle(node, graph, visiting, visited)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasCycle(Long node, Map<Long, List<Long>> graph, Set<Long> visiting, Set<Long> visited) {
        if (visited.contains(node)) {
            return false;
        }
        if (visiting.contains(node)) {
            return true;
        }

        visiting.add(node);
        for (Long child : graph.getOrDefault(node, List.of())) {
            if (hasCycle(child, graph, visiting, visited)) {
                return true;
            }
        }
        visiting.remove(node);
        visited.add(node);
        return false;
    }

    private DependencyResponseDTO toDto(TaskDependency dependency) {
        DependencyResponseDTO dto = new DependencyResponseDTO();
        dto.setId(dependency.getId());
        dto.setOwnerId(dependency.getOwnerId());
        dto.setPredecessorTaskId(dependency.getPredecessorTaskId());
        dto.setSuccessorTaskId(dependency.getSuccessorTaskId());
        dto.setCreatedAt(dependency.getCreatedAt());
        return dto;
    }
}

