package tn.esprit.evaluation.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import tn.esprit.evaluation.entity.FreelancerQuestionTrace;

import java.util.List;
import java.util.Optional;

public interface FreelancerQuestionTraceRepository extends JpaRepository<FreelancerQuestionTrace, Long> {

    Optional<FreelancerQuestionTrace> findByFreelancerIdAndQuestion_Id(Long freelancerId, Long questionId);

    @Query("""
            select t.question.id from FreelancerQuestionTrace t
            where t.freelancerId = :freelancerId
            and t.question.examen.id = :examenId
            and t.wrongCount > 0
            """)
    List<Long> findQuestionIdsRatéesHistoriquement(
            @Param("freelancerId") Long freelancerId,
            @Param("examenId") Long examenId);
}
