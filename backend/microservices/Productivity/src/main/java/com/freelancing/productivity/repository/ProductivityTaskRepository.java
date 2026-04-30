package com.freelancing.productivity.repository;

import com.freelancing.productivity.entity.ProductivityTask;
import com.freelancing.productivity.enums.ProductivityTaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.Instant;
import java.util.List;

public interface ProductivityTaskRepository extends JpaRepository<ProductivityTask, Long>, JpaSpecificationExecutor<ProductivityTask> {
    List<ProductivityTask> findByOwnerIdOrderByDueAtAscCreatedAtDesc(Long ownerId);

    List<ProductivityTask> findByOwnerIdAndStatusInOrderByDueAtAscCreatedAtDesc(Long ownerId, List<ProductivityTaskStatus> statuses);

    List<ProductivityTask> findByOwnerIdAndGoalIdOrderByDueAtAscCreatedAtDesc(Long ownerId, Long goalId);

    List<ProductivityTask> findByOwnerIdAndCompletedAtIsNotNullOrderByCompletedAtDesc(Long ownerId);

    List<ProductivityTask> findByOwnerIdAndStatusIn(Long ownerId, List<ProductivityTaskStatus> statuses);

    long countByOwnerIdAndGoalId(Long ownerId, Long goalId);

    long countByOwnerIdAndGoalIdAndStatus(Long ownerId, Long goalId, ProductivityTaskStatus status);

    List<ProductivityTask> findByOwnerIdAndDueAtBetween(Long ownerId, Instant start, Instant end);
}
