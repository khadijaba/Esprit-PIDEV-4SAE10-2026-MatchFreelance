package com.freelancing.productivity.repository;

import com.freelancing.productivity.entity.ProductivityGoal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductivityGoalRepository extends JpaRepository<ProductivityGoal, Long> {
    List<ProductivityGoal> findByOwnerIdOrderByCreatedAtDesc(Long ownerId);
}

