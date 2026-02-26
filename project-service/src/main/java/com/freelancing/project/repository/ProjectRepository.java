package com.freelancing.project.repository;

import com.freelancing.project.entity.Project;
import com.freelancing.project.enums.ProjectStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface ProjectRepository extends JpaRepository<Project, Long>, JpaSpecificationExecutor<Project> {

    List<Project> findByStatus(ProjectStatus status);

    List<Project> findByTitleContainingIgnoreCase(String title);

    List<Project> findByClientId(Long clientId);

    Page<Project> findByStatus(ProjectStatus status, Pageable pageable);

    Page<Project> findByTitleContainingIgnoreCase(String title, Pageable pageable);

    Page<Project> findByClientId(Long clientId, Pageable pageable);
}
