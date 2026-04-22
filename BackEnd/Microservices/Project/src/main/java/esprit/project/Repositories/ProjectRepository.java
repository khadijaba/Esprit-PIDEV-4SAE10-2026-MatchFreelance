package esprit.project.Repositories;

import esprit.project.entities.Project;
import esprit.project.entities.ProjectStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {

    List<Project> findByStatus(ProjectStatus status);

    List<Project> findByProjectOwnerId(Long projectOwnerId);

    List<Project> findByProjectOwnerIdAndStatus(Long projectOwnerId, ProjectStatus status);

    @Query("SELECT p FROM Project p WHERE LOWER(p.title) LIKE LOWER(CONCAT('%', :title, '%'))")
    List<Project> findByTitleContaining(@Param("title") String title);

    @Query("SELECT p FROM Project p WHERE :skill MEMBER OF p.requiredSkills")
    List<Project> findByRequiredSkillsContaining(@Param("skill") String skill);
<<<<<<< HEAD
=======
<<<<<<< HEAD
=======
>>>>>>> b7e93fa9abcd913d3ba37913b8481d5dd480ed43

    long countByProjectOwnerId(Long projectOwnerId);

    long countByProjectOwnerIdAndStatus(Long projectOwnerId, ProjectStatus status);
<<<<<<< HEAD
=======
>>>>>>> 8d5250d (Ajout du projet MatchFreelance)
>>>>>>> b7e93fa9abcd913d3ba37913b8481d5dd480ed43
}
