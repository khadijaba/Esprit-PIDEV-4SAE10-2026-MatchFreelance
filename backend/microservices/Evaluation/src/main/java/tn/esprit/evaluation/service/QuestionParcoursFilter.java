package tn.esprit.evaluation.service;

import tn.esprit.evaluation.domain.TypeParcours;
import tn.esprit.evaluation.entity.ParcoursInclusion;
import tn.esprit.evaluation.entity.Question;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public final class QuestionParcoursFilter {

    private QuestionParcoursFilter() {
    }

    public static boolean matches(ParcoursInclusion inclusion, TypeParcours typeParcours) {
        ParcoursInclusion inc = inclusion != null ? inclusion : ParcoursInclusion.COMMUN;
        if (inc == ParcoursInclusion.COMMUN) {
            return true;
        }
        if (typeParcours == TypeParcours.STANDARD) {
            return inc == ParcoursInclusion.STANDARD;
        }
        return inc == ParcoursInclusion.RENFORCEMENT;
    }

    public static List<Question> filterForParcours(List<Question> questions, TypeParcours typeParcours) {
        List<Question> filtered = questions.stream()
                .filter(q -> matches(q.getParcoursInclusion(), typeParcours))
                .sorted(Comparator.comparing(Question::getOrdre, Comparator.nullsLast(Comparator.naturalOrder())))
                .collect(Collectors.toList());
        if (filtered.isEmpty() && !questions.isEmpty()) {
            return questions.stream()
                    .sorted(Comparator.comparing(Question::getOrdre, Comparator.nullsLast(Comparator.naturalOrder())))
                    .collect(Collectors.toList());
        }
        return filtered;
    }
}
