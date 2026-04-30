package com.freelancing.productivity.repository;

import com.freelancing.productivity.entity.TaskDependency;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TaskDependencyRepository extends JpaRepository<TaskDependency, Long> {
    List<TaskDependency> findByOwnerId(Long ownerId);

    List<TaskDependency> findByOwnerIdAndSuccessorTaskId(Long ownerId, Long successorTaskId);

    List<TaskDependency> findByOwnerIdAndPredecessorTaskId(Long ownerId, Long predecessorTaskId);

    boolean existsByOwnerIdAndPredecessorTaskIdAndSuccessorTaskId(Long ownerId, Long predecessorTaskId, Long successorTaskId);
}

