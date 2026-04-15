package tn.esprit.evaluation.service;

import java.util.Map;

/**
 * Rédaction de QCM « réels » (énoncé + distracteurs crédibles) à partir des métadonnées module / formation.
 */
public final class ExamenQuestionRedactionUtil {

    private ExamenQuestionRedactionUtil() {
    }

    public static String str(Object o) {
        return o != null ? o.toString() : "";
    }

    public static int parseOrdreModule(Object o) {
        if (o == null) {
            return 0;
        }
        try {
            return Integer.parseInt(o.toString());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public static String shorten(String s, int max) {
        if (s == null || s.length() <= max) {
            return s != null ? s : "";
        }
        return s.substring(0, Math.max(0, max - 3)) + "...";
    }

    public static String themeDepuisModule(String modTitre) {
        return shorten(modTitre == null ? "" : modTitre.trim(), 120);
    }

    /**
     * Énoncé +4 options ; la bonne réponse métier est toujours <strong>A</strong> (affirmation correcte pédagogique).
     */
    public static String[] redigerQcmDepuisModule(String modTitre, String description, int variante) {
        String titre = (modTitre == null || modTitre.isBlank()) ? "Module" : modTitre.trim();
        String titreCourt = shorten(titre, 72);
        String ctx = (description == null || description.isBlank())
                ? ""
                : " Rappel : " + shorten(description.trim(), 180);

        String enonce = switch (variante % 4) {
            case 0 -> "Concernant le module « "
                    + titreCourt
                    + " », quelle affirmation décrit le mieux ce qu'un apprenant doit en retirer ?"
                    + ctx;
            case 1 -> "Pour le module « "
                    + titreCourt
                    + " », laquelle de ces propositions correspond à une attente réaliste de la formation ?"
                    + ctx;
            case 2 -> "Dans le cadre du module « "
                    + titreCourt
                    + " », quelle réponse est la plus cohérente avec un objectif pédagogique sérieux ?"
                    + ctx;
            default -> "À propos de « "
                    + titreCourt
                    + " », identifiez l'affirmation la plus juste du point de vue apprentissage."
                    + ctx;
        };

        String a = "Les objectifs du module sont atteints en suivant les activités proposées et en reliant le contenu à des situations concrètes.";
        String b = "Ce module est purement informatif : aucune compétence n'est attendue après son étude.";
        String c = "Le contenu peut être sauté sans impact sur la compréhension du reste de la formation.";
        String d = "La validation repose uniquement sur la mémorisation de listes, sans mise en pratique.";

        return new String[] { shorten(enonce, 1000), a, b, c, d };
    }

    /** QCM lorsqu'aucun module n'est disponible : ancrage sur le titre de formation. */
    public static String[] redigerQcmDepuisFormationSeule(String formationTitre, int variante) {
        String t = (formationTitre == null || formationTitre.isBlank())
                ? "cette formation"
                : "« " + shorten(formationTitre.trim(), 64) + " »";
        String enonce = switch (variante % 3) {
            case 0 -> "Pour la formation " + t + ", quelle affirmation reflète le mieux une attente raisonnable pour l'apprenant ?";
            case 1 -> "Dans le parcours " + t + ", quelle proposition est la plus cohérente avec un dispositif de formation professionnelle ?";
            default -> "Concernant " + t + ", identifiez l'affirmation la plus juste concernant l'acquisition de compétences.";
        };
        String a = "La formation vise des compétences mobilisables et propose des mises en situation ou exercices pour les ancrer.";
        String b = "La formation ne fixe aucun objectif mesurable : le suivi des acquis est optionnel.";
        String c = "Les contenus sont décorrélés des besoins métier et n'ont pas à être appliqués en contexte réel.";
        String d = "La réussite se limite à la présence aux séances, sans vérification des acquis.";
        return new String[] { shorten(enonce, 1000), a, b, c, d };
    }

    public static Map<String, Object> modulePourIndex(java.util.List<Map<String, Object>> modules, int index) {
        if (modules == null || modules.isEmpty()) {
            return null;
        }
        return modules.get(Math.floorMod(index, modules.size()));
    }
}
