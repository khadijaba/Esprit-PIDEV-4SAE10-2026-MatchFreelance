package tn.esprit.evaluation.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import tn.esprit.evaluation.entity.Question;

import java.util.List;

public interface QuestionRepository extends JpaRepository<Question, Long> {

    List<Question> findByExamenIdOrderByOrdreAsc(Long examenId);

    /** Thèmes pédagogiques distincts (pour compétences complémentaires après certificat). */
    @Query("select distinct q.theme from Question q where q.examen.id = :examenId and q.theme is not null and trim(q.theme) <> ''")
    List<String> findDistinctThemesByExamenId(@Param("examenId") Long examenId);
}
