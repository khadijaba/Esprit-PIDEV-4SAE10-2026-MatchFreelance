package esprit.project.Repositories;

import esprit.project.entities.Project;
import esprit.project.entities.ProjectPhase;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProjectPhaseRepository extends JpaRepository<ProjectPhase, Long> {
    List<ProjectPhase> findByProjectOrderByPhaseOrderAsc(Project project);
    Optional<ProjectPhase> findByProjectAndPhaseOrder(Project project, Integer phaseOrder);
}
