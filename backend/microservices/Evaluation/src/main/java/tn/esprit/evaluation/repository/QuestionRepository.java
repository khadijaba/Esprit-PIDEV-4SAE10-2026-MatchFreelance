package tn.esprit.evaluation.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.esprit.evaluation.entity.Question;

import java.util.List;

public interface QuestionRepository extends JpaRepository<Question, Long> {

    List<Question> findByExamenIdOrderByOrdreAsc(Long examenId);
}
