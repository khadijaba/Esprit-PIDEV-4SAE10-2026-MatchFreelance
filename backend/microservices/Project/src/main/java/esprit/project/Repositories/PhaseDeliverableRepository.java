package esprit.project.Repositories;

import esprit.project.entities.PhaseDeliverable;
import esprit.project.entities.ProjectPhase;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PhaseDeliverableRepository extends JpaRepository<PhaseDeliverable, Long> {
    List<PhaseDeliverable> findByPhase(ProjectPhase phase);
}
