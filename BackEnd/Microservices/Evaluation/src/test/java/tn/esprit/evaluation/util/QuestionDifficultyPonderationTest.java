package tn.esprit.evaluation.util;

import org.junit.jupiter.api.Test;
import tn.esprit.evaluation.domain.NiveauDifficulteQuestion;

import static org.junit.jupiter.api.Assertions.assertEquals;

class QuestionDifficultyPonderationTest {

    @Test
    void poids_parNiveau() {
        assertEquals(1, QuestionDifficultyPonderation.poids(NiveauDifficulteQuestion.FACILE));
        assertEquals(2, QuestionDifficultyPonderation.poids(NiveauDifficulteQuestion.MOYEN));
        assertEquals(3, QuestionDifficultyPonderation.poids(NiveauDifficulteQuestion.DIFFICILE));
        assertEquals(2, QuestionDifficultyPonderation.poids(null));
    }

    @Test
    void pourcentagePlancher() {
        assertEquals(80, QuestionDifficultyPonderation.pourcentagePlancher(4, 5));
        assertEquals(0, QuestionDifficultyPonderation.pourcentagePlancher(1, 0));
    }
}
