package com.freelancing.project.repository;

import com.freelancing.project.entity.Project;
import com.freelancing.project.enums.ProjectStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProjectRepository extends JpaRepository<Project, Long> {

    List<Project> findByStatus(ProjectStatus status);

    List<Project> findByTitleContainingIgnoreCase(String title);

    List<Project> findByClientId(Long clientId);
}
