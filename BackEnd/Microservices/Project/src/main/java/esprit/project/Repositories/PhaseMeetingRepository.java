package esprit.project.Repositories;

import esprit.project.entities.PhaseMeeting;
import esprit.project.entities.ProjectPhase;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PhaseMeetingRepository extends JpaRepository<PhaseMeeting, Long> {
    List<PhaseMeeting> findByPhase(ProjectPhase phase);
}
