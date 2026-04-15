package tn.esprit.evaluation.util;

import java.text.Normalizer;
import java.util.Locale;

/**
 * Normalisation des réponses QCM (A–D) pour un traitement homogène côté API, front et correcteurs.
 */
public final class QcmReponseNormalizer {

    private QcmReponseNormalizer() {
    }

    /**
     * Extrait la lettre A–D à partir d'une saisie libre (ex. « b) », « (C) », espaces, casse).
     */
    public static String extraireLettreReponse(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        String t = raw.trim();
        if ("?".equals(t)) {
            return "";
        }
        String s = Normalizer.normalize(t, Normalizer.Form.NFKC).toUpperCase(Locale.ROOT);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c >= 'A' && c <= 'D') {
                return String.valueOf(c);
            }
        }
        return "";
    }

    public static String extraireLettreBonneReponse(String bonneEnBase) {
        return extraireLettreReponse(bonneEnBase);
    }

    public static boolean reponseQcmCorrecte(String reponseChoisieBrute, String bonneReponseEnBase) {
        String attendu = extraireLettreBonneReponse(bonneReponseEnBase);
        if (attendu.isEmpty()) {
            return false;
        }
        return extraireLettreReponse(reponseChoisieBrute).equals(attendu);
    }

    /**
     * Pourcentage entier par défaut (plancher) : pas d'arrondi favorable pour le candidat.
     */
    public static int pourcentagePlancher(int bonnes, int total) {
        if (total <= 0) {
            return 0;
        }
        return (100 * bonnes) / total;
    }
}
