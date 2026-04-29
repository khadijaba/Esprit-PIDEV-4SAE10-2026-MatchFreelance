package tn.esprit.evaluation.util;

import tn.esprit.evaluation.domain.NiveauDifficulteQuestion;

/**
 * Pondération des questions pour le score final : FACILE 1, MOYEN 2, DIFFICILE 3.
 * Si toutes les questions ont le même niveau, le pourcentage coïncide avec le taux de bonnes réponses.
 */
public final class QuestionDifficultyPonderation {

    private QuestionDifficultyPonderation() {
    }

    public static int poids(NiveauDifficulteQuestion niveau) {
        if (niveau == null) {
            return poids(NiveauDifficulteQuestion.MOYEN);
        }
        return switch (niveau) {
            case FACILE -> 1;
            case MOYEN -> 2;
            case DIFFICILE -> 3;
        };
    }

    public static int pourcentagePlancher(int pointsObtenus, int pointsMax) {
        if (pointsMax <= 0) {
            return 0;
        }
        return (100 * pointsObtenus) / pointsMax;
    }
}
