package com.freelancing.productivity.repository;

import com.freelancing.productivity.entity.DecisionLogEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DecisionLogEntryRepository extends JpaRepository<DecisionLogEntry, Long> {
    List<DecisionLogEntry> findByOwnerIdOrderByCreatedAtDesc(Long ownerId);
}

