package tn.esprit.evaluation.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.evaluation.ExamenMetierConstants;
import tn.esprit.evaluation.client.ExamenLlmClient;
import tn.esprit.evaluation.client.FormationClient;
import tn.esprit.evaluation.domain.NiveauDifficulteQuestion;
import tn.esprit.evaluation.domain.TypeParcours;
import tn.esprit.evaluation.entity.ParcoursInclusion;
import tn.esprit.evaluation.client.ExamenPythonGeneratorClient;
import tn.esprit.evaluation.dto.AutoGenerateExamenRequest;
import tn.esprit.evaluation.dto.ExamenDto;
import tn.esprit.evaluation.dto.QuestionDto;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Génère un examen QCM à partir du catalogue de modules d'une formation (microservice Formation).
 * Les questions sont dérivées du titre et du descriptif des modules (affirmations plausibles + distracteurs).
 * Une relecture formateur reste recommandée avant certification.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ExamenGenerationService {

    private static final int MAX_ENONCE = 1000;
    private static final int MAX_OPTION = 500;
    private static final int MAX_EXPLICATION = 2000;
    private static final Pattern SENTENCE_END = Pattern.compile("(?<=[.!?])\\s+");

    private final FormationClient formationClient;
    private final ExamenService examenService;
    private final ExamenPythonGeneratorClient examenPythonGeneratorClient;
    private final ExamenLlmClient examenLlmClient;

    /**
     * Si {@code true} : quand le LLM est demandé et échoue, on ne renvoie plus un examen « gabarit » (toutes les réponses A, variantes).
     * Erreur explicite pour éviter de croire que l’IA a fonctionné.
     */
    @Value("${app.examen.llm.reject-on-llm-failure:true}")
    private boolean rejectOnLlmFailure;

    @Transactional
    public ExamenDto genererDepuisFormation(Long formationId, AutoGenerateExamenRequest req) {
        if (req == null) {
            req = AutoGenerateExamenRequest.builder().build();
        }
        Map<String, Object> formation = formationClient.getFormationById(formationId);
        if (formation == null || formation.isEmpty()) {
            throw new RuntimeException("Formation introuvable ou service Formation indisponible (id=" + formationId + ").");
        }
        List<Map<String, Object>> modules = formationClient.getModulesByFormation(formationId);
        if (modules == null || modules.isEmpty()) {
            throw new RuntimeException(
                    "Aucun module pour cette formation : impossible de générer un examen automatique. Ajoutez des modules côté Formation.");
        }

        modules.sort(Comparator.comparingInt(m -> parseOrdre(m.get("ordre"))));

        String titreFormation = str(formation.get("titre"));
        if (titreFormation.isBlank()) {
            titreFormation = "Formation " + formationId;
        }
        /*
         * Si le LLM Java (Ollama / OpenAI) est disponible et demandé, ne pas appeler Python en premier :
         * sinon Python renvoie souvent un examen heuristique (sans son propre LLM) et on retournait tout de suite,
         * sans jamais exécuter le LLM Java — d’où questions « Variante 1…9 » et « Sans descriptif de module ».
         */
        boolean useJavaLlmPath = shouldUseLlm(req) && examenLlmClient.isAvailable();
        log.info(
                "Génération examen formationId={} preview={} useLlm(demande)={} shouldUseLlm={} llmDisponible={} pythonUrlConfigure={} cheminJavaLlmPrioritaire={}",
                formationId,
                req.getPreview(),
                req.getUseLlm(),
                shouldUseLlm(req),
                examenLlmClient.isAvailable(),
                examenPythonGeneratorClient.isConfigured(),
                useJavaLlmPath);
        if (useJavaLlmPath) {
            log.info(
                    "Génération examen : LLM Java activé pour cette requête — le générateur Python n'est pas utilisé (évite un examen 100 % heuristique).");
        }
        if (!useJavaLlmPath && examenPythonGeneratorClient.isConfigured()) {
            Boolean useLlmForPython = req.getUseLlm();
            if (useLlmForPython == null && examenLlmClient.isUseByDefault()) {
                useLlmForPython = Boolean.TRUE;
            }
            ExamenDto fromPython = examenPythonGeneratorClient.generate(
                    formationId,
                    titreFormation,
                    modules,
                    req.getSuffixeTitre(),
                    req.getSeuilReussi(),
                    useLlmForPython);
            if (fromPython != null
                    && fromPython.getQuestions() != null
                    && !fromPython.getQuestions().isEmpty()) {
                fromPython.setFormationId(formationId);
                return persistOrPreview(fromPython, req);
            }
        }

        String titreExamen = "Examen — " + titreFormation;
        if (req.getSuffixeTitre() != null && !req.getSuffixeTitre().isBlank()) {
            titreExamen = titreExamen + " " + req.getSuffixeTitre().trim();
        }

        int nbModulesSource = modules.size();
        List<ExamenLlmClient.QuestionGenMeta> metas = collectMetas(modules);
        boolean llmUsed = false;
        List<QuestionDto> questions;
        if (shouldUseLlm(req)) {
            List<QuestionDto> fromLlm = examenLlmClient.generateQuestions(titreFormation, metas);
            if (fromLlm != null && fromLlm.size() == metas.size()) {
                questions = fromLlm;
                llmUsed = true;
            } else {
                if (shouldUseLlm(req)) {
                    String msg =
                            "Génération examen : le LLM n'a pas renvoyé un jeu complet valide. Consultez les logs Evaluation (lignes « LLM: »). "
                                    + "Vérifiez Ollama (URL, modèle), la réponse HTTP et le JSON des questions.";
                    log.error(msg);
                    if (rejectOnLlmFailure) {
                        throw new IllegalStateException(msg);
                    }
                    log.warn("Repli sur le modèle heuristique (désactivez app.examen.llm.reject-on-llm-failure pour autoriser ce repli).");
                }
                questions = heuristicFromMetas(metas, nbModulesSource);
            }
        } else {
            questions = heuristicFromMetas(metas, nbModulesSource);
        }

        String descExamen = "Examen généré automatiquement à partir des titres et descriptifs des modules. "
                + "Au moins " + ExamenMetierConstants.MIN_QUESTIONS_PAR_EXAMEN
                + " questions par parcours (Standard et Renforcement). ";
        if (llmUsed) {
            descExamen += "Questions rédigées avec assistance LLM (relecture obligatoire). ";
        }
        descExamen += "Relecture formateur recommandée avant usage certifiant.";

        ExamenDto dto = ExamenDto.builder()
                .formationId(formationId)
                .titre(titreExamen)
                .description(descExamen)
                .seuilReussi(req.getSeuilReussi() != null ? req.getSeuilReussi() : 60)
                .questions(questions)
                .build();

        return persistOrPreview(dto, req);
    }

    private static boolean isPreview(AutoGenerateExamenRequest req) {
        return Boolean.TRUE.equals(req.getPreview());
    }

    private ExamenDto persistOrPreview(ExamenDto dto, AutoGenerateExamenRequest req) {
        if (isPreview(req)) {
            return stripIdsForPreview(dto);
        }
        return examenService.create(dto);
    }

    private static ExamenDto stripIdsForPreview(ExamenDto dto) {
        if (dto == null) {
            return null;
        }
        dto.setId(null);
        if (dto.getQuestions() != null) {
            dto.getQuestions().forEach(q -> {
                q.setId(null);
                q.setExamenId(null);
            });
        }
        return dto;
    }

    private boolean shouldUseLlm(AutoGenerateExamenRequest req) {
        if (!examenLlmClient.isAvailable()) {
            return false;
        }
        if (Boolean.FALSE.equals(req.getUseLlm())) {
            return false;
        }
        if (Boolean.TRUE.equals(req.getUseLlm())) {
            return true;
        }
        return examenLlmClient.isUseByDefault();
    }

    private List<ExamenLlmClient.QuestionGenMeta> collectMetas(List<Map<String, Object>> modules) {
        List<ExamenLlmClient.QuestionGenMeta> metas = new ArrayList<>();
        int ordre = 0;
        int nMod = modules.size();
        for (Map<String, Object> mod : modules) {
            String modTitre = str(mod.get("titre"));
            if (modTitre.isBlank()) {
                modTitre = "Module " + (ordre + 1);
            }
            String desc = str(mod.get("description"));
            metas.add(new ExamenLlmClient.QuestionGenMeta(
                    ordre,
                    modTitre,
                    desc,
                    parcoursPourOrdre(ordre),
                    difficultePourOrdre(ordre).name(),
                    shorten(modTitre, 120)));
            ordre++;
        }
        while (metas.size() < ExamenMetierConstants.MIN_QUESTIONS_PAR_EXAMEN) {
            Map<String, Object> mod = modules.get(ordre % nMod);
            String modTitre = str(mod.get("titre"));
            if (modTitre.isBlank()) {
                modTitre = "Module " + (ordre % nMod + 1);
            }
            String desc = str(mod.get("description"));
            metas.add(new ExamenLlmClient.QuestionGenMeta(
                    ordre,
                    modTitre,
                    desc,
                    parcoursPourOrdre(ordre),
                    difficultePourOrdre(ordre).name(),
                    shorten(modTitre, 120)));
            ordre++;
        }
        while (countMetaPourParcours(metas, TypeParcours.STANDARD) < ExamenMetierConstants.MIN_QUESTIONS_PAR_EXAMEN
                || countMetaPourParcours(metas, TypeParcours.RENFORCEMENT)
                        < ExamenMetierConstants.MIN_QUESTIONS_PAR_EXAMEN) {
            Map<String, Object> mod = modules.get(ordre % nMod);
            String modTitre = str(mod.get("titre"));
            if (modTitre.isBlank()) {
                modTitre = "Module " + (ordre % nMod + 1);
            }
            String desc = str(mod.get("description"));
            metas.add(new ExamenLlmClient.QuestionGenMeta(
                    ordre,
                    modTitre,
                    desc,
                    "COMMUN",
                    difficultePourOrdre(ordre).name(),
                    shorten(modTitre, 120)));
            ordre++;
        }
        return metas;
    }

    private static long countMetaPourParcours(List<ExamenLlmClient.QuestionGenMeta> metas, TypeParcours typeParcours) {
        return metas.stream()
                .filter(m -> QuestionParcoursFilter.matches(parseInclusionDto(m.inclusion()), typeParcours))
                .count();
    }

    private List<QuestionDto> heuristicFromMetas(List<ExamenLlmClient.QuestionGenMeta> metas, int nbModulesSource) {
        List<QuestionDto> questions = new ArrayList<>();
        for (ExamenLlmClient.QuestionGenMeta m : metas) {
            boolean variante = m.ordre() >= nbModulesSource;
            questions.add(construireQuestionDepuisModule(
                    m.modTitre(),
                    m.desc(),
                    m.ordre(),
                    m.inclusion(),
                    NiveauDifficulteQuestion.valueOf(m.niveauDifficulte()),
                    m.theme(),
                    variante));
        }
        return questions;
    }

    private static ParcoursInclusion parseInclusionDto(String raw) {
        if (raw == null || raw.isBlank()) {
            return ParcoursInclusion.COMMUN;
        }
        try {
            return ParcoursInclusion.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return ParcoursInclusion.COMMUN;
        }
    }

    private static long countDtoPourParcours(List<QuestionDto> qs, TypeParcours typeParcours) {
        return qs.stream()
                .filter(q -> QuestionParcoursFilter.matches(parseInclusionDto(q.getParcoursInclusion()), typeParcours))
                .count();
    }

    private static String parcoursPourOrdre(int ordre) {
        return switch (ordre % 3) {
            case 0 -> "COMMUN";
            case 1 -> "STANDARD";
            default -> "RENFORCEMENT";
        };
    }

    /** Répartition cyclique FACILE / MOYEN / DIFFICILE pour l’examen adaptatif. */
    private static NiveauDifficulteQuestion difficultePourOrdre(int ordre) {
        return switch (ordre % 3) {
            case 0 -> NiveauDifficulteQuestion.FACILE;
            case 1 -> NiveauDifficulteQuestion.MOYEN;
            default -> NiveauDifficulteQuestion.DIFFICILE;
        };
    }

    private static QuestionDto construireQuestionDepuisModule(
            String modTitre,
            String description,
            int ordre,
            String inclusion,
            NiveauDifficulteQuestion niveau,
            String theme,
            boolean variante) {
        List<String> phrases = extrairePhrases(description);
        int idx = phrases.isEmpty() ? 0 : ordre % phrases.size();
        String enonceBase;
        if (phrases.isEmpty()) {
            enonceBase = "Concernant le module « " + modTitre + " », quelle proposition reflète le mieux le rôle attendu de cette séquence dans le parcours ?";
        } else {
            enonceBase = switch (ordre % 3) {
                case 0 -> "À propos du module « " + modTitre + " », quelle proposition est la plus fidèle au contenu prévu pour les apprenants ?";
                case 1 -> "Pour la séquence « " + modTitre + " », laquelle des affirmations correspond le mieux à ce qui est décrit dans le programme ?";
                default -> "Dans le cadre du module « " + modTitre + " », identifiez l'affirmation qui repose correctement sur le descriptif publié.";
            };
        }
        if (variante) {
            enonceBase = enonceBase + " (Variante " + (ordre + 1) + ".)";
        }
        String optA;
        String optB;
        String optC;
        String optD;
        String explication;
        if (!phrases.isEmpty()) {
            String s0 = phrases.get(idx % phrases.size()).replace("\"", "'");
            optA = "D'après le descriptif, on peut retenir notamment : « " + shorten(s0, 380) + " ».";
            optB = "Le module est présenté comme sans lien avec « " + shorten(modTitre, 80) + " » et n'aborde aucun objectif du programme.";
            optC = "Il est indiqué qu'aucune notion de « " + shorten(modTitre, 100) + " » n'y est travaillée : séance purement informative, hors compétences visées.";
            if (phrases.size() > 1) {
                String s1 = phrases.get((idx + 1) % phrases.size()).replace("\"", "'");
                optD = "Le programme exclut explicitement tout contenu du type : « " + shorten(s1, 360) + " ».";
            } else {
                optD = "Le descriptif précise qu'aucune mise en pratique n'est prévue pour « " + shorten(modTitre, 120) + " ».";
            }
            explication = "La bonne réponse s'appuie sur une formulation du descriptif du module « " + shorten(modTitre, 80) + " ». Contrôlez l'exactitude avant certification.";
        } else {
            optA = "Le module « " + shorten(modTitre, 220) + " » fait partie du parcours et contribue aux objectifs de la formation sur ce thème.";
            optB = "Ce module est optionnel : on peut omettre cette séquence sans impact sur la suite du parcours.";
            optC = "Il est réservé aux apprenants ayant déjà une certification externe équivalente, et non à tous les inscrits.";
            optD = "Il remplace l'ensemble des autres modules et annule les évaluations déjà réalisées dans la formation.";
            explication = "Sans descriptif de module, la réponse attendue reflète le fait que la séquence est bien au programme. Enrichissez les textes des modules pour des QCM plus précis.";
        }
        return QuestionDto.builder()
                .ordre(ordre)
                .enonce(shorten(enonceBase, MAX_ENONCE))
                .optionA(shorten(optA, MAX_OPTION))
                .optionB(shorten(optB, MAX_OPTION))
                .optionC(shorten(optC, MAX_OPTION))
                .optionD(shorten(optD, MAX_OPTION))
                .bonneReponse("A")
                .parcoursInclusion(inclusion)
                .niveauDifficulte(niveau.name())
                .theme(theme)
                .skill(theme)
                .explication(shorten(explication, MAX_EXPLICATION))
                .build();
    }

    private static List<String> extrairePhrases(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        String[] morceaux = SENTENCE_END.split(text.trim());
        List<String> out = new ArrayList<>();
        for (String p : morceaux) {
            String t = p.trim();
            if (t.length() > 12) {
                out.add(t);
            }
        }
        return out;
    }

    private static int parseOrdre(Object o) {
        if (o == null) return 0;
        try {
            return Integer.parseInt(o.toString());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static String str(Object o) {
        return o != null ? o.toString() : "";
    }

    private static String shorten(String s, int max) {
        if (s == null || s.length() <= max) return s;
        return s.substring(0, Math.max(0, max - 3)) + "...";
    }
}
