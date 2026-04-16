package esprit.project.ml;

import esprit.project.entities.Project;

import java.util.List;
import java.util.Optional;

/**
 * Vecteur de features pour le modèle ONNX — ordre et normalisation alignés avec {@code ml/train_project_risk.py}.
 */
public final class ProjectRiskFeatureVector {

    public static final int DIMENSION = 7;

    private ProjectRiskFeatureVector() {
    }

    public static float[] build(Project p, long ownerCompleted, long ownerTotal) {
        int titleLen = Optional.ofNullable(p.getTitle()).map(String::length).orElse(0);
        int wordCount = wordCount(p.getDescription());
        int skillCount = Optional.ofNullable(p.getRequiredSkills()).map(List::size).orElse(0);
        double budget = p.getBudget() != null ? p.getBudget() : 0.0;
        int duration = p.getDuration() != null && p.getDuration() > 0 ? p.getDuration() : 1;
        double daily = budget / duration;
        double completion = ownerTotal > 0 ? (double) ownerCompleted / ownerTotal : 0.55;

        return new float[]{
                titleLen / 100.0f,
                wordCount / 200.0f,
                skillCount / 10.0f,
                (float) (Math.min(daily / 200.0, 2.0) / 2.0),
                (float) (Math.log1p(budget) / 15.0),
                Math.min(duration / 365.0f, 1.0f),
                (float) completion,
        };
    }

    private static int wordCount(String text) {
        if (text == null || text.isBlank()) {
            return 0;
        }
        return text.trim().split("\\s+").length;
    }
}
