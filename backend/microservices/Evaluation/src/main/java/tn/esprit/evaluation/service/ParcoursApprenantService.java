package tn.esprit.evaluation.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.evaluation.client.FormationClient;
import tn.esprit.evaluation.client.ExamenLlmClient;
import tn.esprit.evaluation.domain.TypeParcours;
import tn.esprit.evaluation.dto.ModuleRevisionDto;
import tn.esprit.evaluation.dto.RemediationPlanDto;
import tn.esprit.evaluation.dto.RemediationStepDto;
import tn.esprit.evaluation.dto.RiskEvaluationDto;
import tn.esprit.evaluation.dto.SuccessPredictionDto;
import tn.esprit.evaluation.entity.Examen;
import tn.esprit.evaluation.entity.PassageExamen;
import tn.esprit.evaluation.entity.Question;
import tn.esprit.evaluation.exception.ResourceNotFoundException;
import tn.esprit.evaluation.repository.ExamenRepository;
import tn.esprit.evaluation.repository.PassageExamenRepository;
import tn.esprit.evaluation.repository.QuestionRepository;
import tn.esprit.evaluation.util.QcmReponseNormalizer;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Parcours apprenant : risque d'échec (règles), recommandations de révision (modules Formation).
 */
@Service
@RequiredArgsConstructor
public class ParcoursApprenantService {

    private static final int SEUIL_ALERTE_FORMATEUR = 55;

    private final ExamenRepository examenRepository;
    private final PassageExamenRepository passageExamenRepository;
    private final QuestionRepository questionRepository;
    private final FormationClient formationClient;
    private final ExamenLlmClient examenLlmClient;
    private final FreelancerQuestionTraceService freelancerQuestionTraceService;

    @Transactional(readOnly = true)
    public RiskEvaluationDto evaluerRisque(Long freelancerId, Long examenId) {
        Examen examen = examenRepository.findById(examenId)
                .orElseThrow(() -> new ResourceNotFoundException("Examen", examenId));
        Long formationId = examen.getFormationId();

        List<PassageExamen> historique = passageExamenRepository.findByFreelancerIdAndFormationId(freelancerId, formationId);
        long nbEchecs = historique.stream().filter(p -> p.getResultat() == PassageExamen.ResultatExamen.ECHOUE).count();
        double moyenne = historique.stream().mapToInt(PassageExamen::getScore).average().orElse(0);

        LocalDateTime dateInscription = resolveDateInscription(freelancerId, formationId);
        long joursDepuisInscription = dateInscription != null
                ? ChronoUnit.DAYS.between(dateInscription, LocalDateTime.now())
                : 999;

        int totalMinutesModules = formationClient.getModulesByFormation(formationId).stream()
                .mapToInt(m -> parseInt(m.get("dureeMinutes"), 0))
                .sum();

        List<String> gaps = gapsDetectesSansMicroserviceSkill(freelancerId);
        int nbGaps = gaps.size();

        int score = 0;
        List<String> facteurs = new ArrayList<>();

        if (nbEchecs >= 1) {
            score += 28;
            facteurs.add(nbEchecs + " échec(s) déjà enregistré(s) sur cette formation.");
        }
        if (nbEchecs >= 2) {
            score += 15;
            facteurs.add("Plusieurs échecs : renforcement nécessaire.");
        }
        if (!historique.isEmpty() && moyenne < 55) {
            score += 18;
            facteurs.add(String.format(Locale.FRANCE, "Moyenne historique faible (%.0f %%).", moyenne));
        }
        if (joursDepuisInscription < 14 && dateInscription != null) {
            score += 12;
            facteurs.add("Inscription récente : moins de 2 semaines de préparation.");
        }
        if (totalMinutesModules >= 300 && joursDepuisInscription < 10 && dateInscription != null) {
            score += 10;
            facteurs.add("Volume de modules élevé par rapport au temps depuis l'inscription.");
        }
        if (nbGaps >= 3) {
            score += 12;
            facteurs.add("Plusieurs signaux de préparation à renforcer (sans annuaire Skill).");
        } else if (nbGaps >= 1) {
            score += 5;
            facteurs.add("Axes de révision à consolider avant l'examen.");
        }

        score = Math.min(100, score);
        if (facteurs.isEmpty()) {
            facteurs.add("Profil stable : pas de signal d'alerte particulier.");
        }

        String niveau = score >= 70 ? "ELEVE" : score >= 40 ? "MODERE" : "FAIBLE";
        TypeParcours recommande = score >= 45 ? TypeParcours.RENFORCEMENT : TypeParcours.STANDARD;

        String msgApprenant;
        if (score >= 70) {
            msgApprenant = "Le dispositif recommande le parcours renforcement : prends le temps de réviser les modules suggérés et utilise le mode entraînement si besoin.";
        } else if (score >= 40) {
            msgApprenant = "Risque modéré : un parcours renforcement peut t'aider à sécuriser le passage. À toi de choisir selon ton ressenti.";
        } else {
            msgApprenant = "Les indicateurs sont rassurants : le parcours standard convient en général. Tu peux tout de même choisir le renforcement pour plus de questions ciblées.";
        }

        boolean alerte = score >= SEUIL_ALERTE_FORMATEUR;
        String msgFormateur = alerte
                ? String.format(Locale.FRANCE,
                "Freelancer %d — examen %d (formation %d) : score de risque %d/100 (%s). Envisager un suivi ou un parcours renforcement.",
                freelancerId, examenId, formationId, score, niveau)
                : null;

        return RiskEvaluationDto.builder()
                .scoreRisque(score)
                .niveau(niveau)
                .messageApprenant(msgApprenant)
                .alerteFormateur(alerte)
                .messageFormateur(msgFormateur)
                .facteurs(facteurs)
                .parcoursRecommande(recommande)
                .build();
    }

    public List<ModuleRevisionDto> proposerModulesRevision(
            List<Question> questionsDansLOrdre,
            List<String> reponses,
            Long formationId) {

        List<String> themes = new ArrayList<>();
        for (int i = 0; i < questionsDansLOrdre.size(); i++) {
            String brute = i < reponses.size() ? reponses.get(i) : null;
            Question q = questionsDansLOrdre.get(i);
            if (!QcmReponseNormalizer.reponseQcmCorrecte(brute, q.getBonneReponse())
                    && q.getTheme() != null && !q.getTheme().isBlank()) {
                themes.add(q.getTheme().trim());
            }
        }

        List<Map<String, Object>> modules = formationClient.getModulesByFormation(formationId);
        if (modules.isEmpty()) {
            return List.of();
        }

        if (themes.isEmpty()) {
            return modules.stream()
                    .sorted(Comparator.comparingInt(m -> parseInt(m.get("ordre"), 0)))
                    .limit(3)
                    .map(m -> toModuleDto(m, "Révision générale — parcours de la formation"))
                    .collect(Collectors.toList());
        }

        Map<Long, ScoredModule> best = new LinkedHashMap<>();
        for (String theme : themes.stream().distinct().limit(8).collect(Collectors.toList())) {
            String t = theme.toLowerCase(Locale.FRANCE);
            for (Map<String, Object> m : modules) {
                int score = scoreModulePourTheme(m, t);
                if (score <= 0) continue;
                Long id = parseLong(m.get("id"));
                if (id == null) continue;
                ModuleRevisionDto dto = toModuleDto(m, "Thème à renforcer : " + theme);
                ScoredModule exist = best.get(id);
                if (exist == null || score > exist.score) {
                    best.put(id, new ScoredModule(dto, score));
                }
            }
        }

        return best.values().stream()
                .sorted(Comparator.comparingInt((ScoredModule sm) -> sm.score).reversed())
                .limit(3)
                .map(sm -> sm.dto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public SuccessPredictionDto simulerReussiteAvantPassage(Long freelancerId, Long examenId) {
        Examen examen = examenRepository.findById(examenId)
                .orElseThrow(() -> new ResourceNotFoundException("Examen", examenId));
        Long formationId = examen.getFormationId();

        List<PassageExamen> historique = passageExamenRepository.findByFreelancerIdAndFormationId(freelancerId, formationId);
        int avgScore = (int) Math.round(historique.stream().mapToInt(PassageExamen::getScore).average().orElse(0.0));
        int successRate = historique.isEmpty()
                ? 0
                : (int) Math.round(100.0 * historique.stream()
                .filter(p -> p.getResultat() == PassageExamen.ResultatExamen.REUSSI).count() / historique.size());

        int avgPrepDays = computeAvgPrepDays(historique, resolveDateInscription(freelancerId, formationId));
        int prepTempoScore = Math.max(20, Math.min(100, avgPrepDays * 8));
        int nbGaps = gapsDetectesSansMicroserviceSkill(freelancerId).size();
        int skillReadiness = Math.max(0, 100 - (nbGaps * 12));

        int probabilite = clampScore((int) Math.round(avgScore * 0.55 + successRate * 0.25 + prepTempoScore * 0.10 + skillReadiness * 0.10));
        int scoreMetier = clampScore((int) Math.round(avgScore * 0.45 + successRate * 0.25 + skillReadiness * 0.20 + prepTempoScore * 0.10));
        String niveau = probabilite >= 75 ? "FORTE" : probabilite >= 50 ? "MOYENNE" : "FAIBLE";

        List<ModuleRevisionDto> modules = List.of();
        if (probabilite < 75) {
            List<String> motsCles = new ArrayList<>(gapsDetectesSansMicroserviceSkill(freelancerId));
            modules = proposerModulesDepuisMotsCles(formationId, motsCles, 2);
        }

        String reco = probabilite >= 75
                ? "Vous pouvez tenter le passage certifiant. Faites une révision légère avant l'examen."
                : probabilite >= 50
                ? "Probabilité intermédiaire : faites 1 à 2 modules ciblés avant de passer en certifiant."
                : "Probabilité faible : faites au moins 2 modules de remédiation puis lancez un entraînement.";

        String explicationNaturelle = buildNaturalCoachExplanation(
                freelancerId, examenId, probabilite, scoreMetier, avgScore, successRate, avgPrepDays, nbGaps, reco);

        return SuccessPredictionDto.builder()
                .probabiliteReussite(probabilite)
                .niveauConfiance(niveau)
                .scoreMoyenHistorique(avgScore)
                .tauxReussiteHistorique(successRate)
                .tempsMoyenPreparationJours(avgPrepDays)
                .scoreMetierRealiste(scoreMetier)
                .explicationNaturelle(explicationNaturelle)
                .recommandation(reco)
                .modulesAvantCertifiant(modules)
                .build();
    }

    @Transactional(readOnly = true)
    public RemediationPlanDto construirePlanRemediation(Long freelancerId, Long examenId) {
        Examen examen = examenRepository.findById(examenId)
                .orElseThrow(() -> new ResourceNotFoundException("Examen", examenId));
        RiskEvaluationDto risque = evaluerRisque(freelancerId, examenId);

        Set<Long> questionIdsRatees = new HashSet<>(freelancerQuestionTraceService
                .listerQuestionIdsRatéesHistoriquement(freelancerId, examenId));
        List<Question> questions = questionRepository.findByExamenIdOrderByOrdreAsc(examenId);
        List<String> themesFaibles = questions.stream()
                .filter(q -> q.getId() != null && questionIdsRatees.contains(q.getId()))
                .map(Question::getTheme)
                .filter(t -> t != null && !t.isBlank())
                .map(String::trim)
                .distinct()
                .limit(8)
                .collect(Collectors.toList());

        List<String> motsCles = new ArrayList<>(themesFaibles);
        if (motsCles.isEmpty()) {
            motsCles.addAll(gapsDetectesSansMicroserviceSkill(freelancerId));
        }

        List<ModuleRevisionDto> modules = proposerModulesDepuisMotsCles(examen.getFormationId(), motsCles, 6);
        if (modules.isEmpty()) {
            modules = formationClient.getModulesByFormation(examen.getFormationId()).stream()
                    .sorted(Comparator.comparingInt(m -> parseInt(m.get("ordre"), 0)))
                    .limit(4)
                    .map(m -> toModuleDto(m, "Consolidation générale avant nouvelle tentative"))
                    .collect(Collectors.toList());
        }

        int objectifScore = risque.getScoreRisque() >= 70 ? 80 : risque.getScoreRisque() >= 40 ? 75 : 70;
        List<RemediationStepDto> etapes = new ArrayList<>();
        int totalMinutes = 0;
        LocalDateTime now = LocalDateTime.now();
        for (int i = 0; i < modules.size(); i++) {
            ModuleRevisionDto m = modules.get(i);
            int duree = m.getDureeMinutes() != null && m.getDureeMinutes() > 0 ? m.getDureeMinutes() : 45;
            totalMinutes += duree;
            etapes.add(RemediationStepDto.builder()
                    .sequence(i + 1)
                    .moduleId(m.getId())
                    .moduleTitre(m.getTitre())
                    .dureeEstimeeMinutes(duree)
                    .dateCible(now.toLocalDate().plusDays((long) i * 2L + 1))
                    .objectifScoreTheme(objectifScore)
                    .raison(m.getRaison())
                    .build());
        }

        String resume = String.format(
                Locale.FRANCE,
                "Learning path personnalisé : %d étape(s), environ %d min de révision. Parcours conseillé : %s.",
                etapes.size(), totalMinutes, risque.getParcoursRecommande() != null ? risque.getParcoursRecommande() : TypeParcours.STANDARD);

        return RemediationPlanDto.builder()
                .freelancerId(freelancerId)
                .examenId(examenId)
                .generatedAt(now)
                .parcoursSuggere(risque.getParcoursRecommande() != null ? risque.getParcoursRecommande() : TypeParcours.STANDARD)
                .objectifScoreCible(objectifScore)
                .estimationTotaleMinutes(totalMinutes)
                .progressionPourcent(0)
                .resume(resume)
                .etapes(etapes)
                .build();
    }

    private record ScoredModule(ModuleRevisionDto dto, int score) {
    }

    private static int scoreModulePourTheme(Map<String, Object> m, String themeLower) {
        String titre = str(m.get("titre")).toLowerCase(Locale.FRANCE);
        String desc = str(m.get("description")).toLowerCase(Locale.FRANCE);
        int s = 0;
        if (titre.contains(themeLower)) s += 3;
        if (desc.contains(themeLower)) s += 2;
        for (String part : themeLower.split("[\\s,;/]+")) {
            if (part.length() < 3) continue;
            if (titre.contains(part)) s += 2;
            if (desc.contains(part)) s += 1;
        }
        return s;
    }

    private ModuleRevisionDto toModuleDto(Map<String, Object> m, String raison) {
        return ModuleRevisionDto.builder()
                .id(parseLong(m.get("id")))
                .titre(str(m.get("titre")))
                .description(shorten(str(m.get("description")), 200))
                .ordre(parseInt(m.get("ordre"), 0))
                .dureeMinutes(parseInt(m.get("dureeMinutes"), 0))
                .raison(raison)
                .build();
    }

    private List<ModuleRevisionDto> proposerModulesDepuisMotsCles(Long formationId, List<String> motsCles, int limit) {
        List<Map<String, Object>> modules = formationClient.getModulesByFormation(formationId);
        if (modules.isEmpty()) {
            return List.of();
        }
        if (motsCles == null || motsCles.isEmpty()) {
            return modules.stream()
                    .sorted(Comparator.comparingInt(m -> parseInt(m.get("ordre"), 0)))
                    .limit(Math.max(1, limit))
                    .map(m -> toModuleDto(m, "Révision conseillée avant passage"))
                    .collect(Collectors.toList());
        }
        Map<Long, ScoredModule> best = new LinkedHashMap<>();
        for (String kw : motsCles.stream().filter(s -> s != null && !s.isBlank()).map(String::trim).distinct().toList()) {
            String lower = kw.toLowerCase(Locale.FRANCE);
            for (Map<String, Object> m : modules) {
                int score = scoreModulePourTheme(m, lower);
                if (score <= 0) {
                    continue;
                }
                Long id = parseLong(m.get("id"));
                if (id == null) {
                    continue;
                }
                ModuleRevisionDto dto = toModuleDto(m, "Lacune détectée : " + kw);
                ScoredModule existing = best.get(id);
                if (existing == null || score > existing.score) {
                    best.put(id, new ScoredModule(dto, score));
                }
            }
        }
        if (best.isEmpty()) {
            return modules.stream()
                    .sorted(Comparator.comparingInt(m -> parseInt(m.get("ordre"), 0)))
                    .limit(Math.max(1, limit))
                    .map(m -> toModuleDto(m, "Consolidation recommandée"))
                    .collect(Collectors.toList());
        }
        return best.values().stream()
                .sorted(Comparator.comparingInt((ScoredModule sm) -> sm.score).reversed())
                .limit(Math.max(1, limit))
                .map(sm -> sm.dto)
                .collect(Collectors.toList());
    }

    private LocalDateTime resolveDateInscription(Long freelancerId, Long formationId) {
        for (Map<String, Object> row : formationClient.getInscriptionsByFreelancer(freelancerId)) {
            Object fid = row.get("formationId");
            if (fid != null && formationId.equals(Long.valueOf(fid.toString()))) {
                Object d = row.get("dateInscription");
                if (d != null) {
                    try {
                        return LocalDateTime.parse(d.toString().replace(" ", "T"));
                    } catch (Exception ignored) {
                        return null;
                    }
                }
            }
        }
        return null;
    }

    private static String str(Object o) {
        return o != null ? o.toString() : "";
    }

    private static int parseInt(Object o, int def) {
        if (o == null) return def;
        try {
            return Integer.parseInt(o.toString());
        } catch (NumberFormatException e) {
            return def;
        }
    }

    private static Long parseLong(Object o) {
        if (o == null) return null;
        try {
            return Long.valueOf(o.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String shorten(String s, int max) {
        if (s == null || s.length() <= max) return s;
        return s.substring(0, max - 1) + "…";
    }

    private static int clampScore(int s) {
        return Math.max(0, Math.min(100, s));
    }

    private static int computeAvgPrepDays(List<PassageExamen> historique, LocalDateTime dateInscription) {
        if (historique == null || historique.isEmpty()) {
            return 7;
        }
        List<LocalDateTime> dates = historique.stream()
                .map(PassageExamen::getDatePassage)
                .filter(d -> d != null)
                .sorted()
                .collect(Collectors.toList());
        if (dates.size() >= 2) {
            long sum = 0;
            int n = 0;
            for (int i = 1; i < dates.size(); i++) {
                long days = Math.max(0, ChronoUnit.DAYS.between(dates.get(i - 1), dates.get(i)));
                sum += days;
                n++;
            }
            if (n > 0) {
                return (int) Math.max(1, Math.round((double) sum / n));
            }
        }
        if (dateInscription != null) {
            long days = Math.max(1, ChronoUnit.DAYS.between(dateInscription, LocalDateTime.now()));
            return (int) Math.max(1, Math.round((double) days / Math.max(1, dates.size())));
        }
        return 7;
    }

    private String buildNaturalCoachExplanation(
            Long freelancerId,
            Long examenId,
            int probabilite,
            int scoreMetier,
            int moyenne,
            int tauxReussite,
            int prepJours,
            int nbGaps,
            String recommandation) {
        String fallback = String.format(
                Locale.FRANCE,
                "Votre probabilité de réussite est estimée à %d%% (score métier %d/100). " +
                        "Historique: moyenne %d%%, réussite %d%%, préparation moyenne %d jour(s), gaps détectés: %d. %s",
                probabilite, scoreMetier, moyenne, tauxReussite, prepJours, nbGaps, recommandation);
        try {
            String prompt = String.format(
                    Locale.FRANCE,
                    "Freelancer %d, examen %d. Probabilité %d%%, score métier %d/100, moyenne historique %d%%, " +
                            "taux de réussite %d%%, préparation moyenne %d jours, gaps détectés %d. " +
                            "Recommandation système: %s. Rédige une explication courte et actionnable.",
                    freelancerId, examenId, probabilite, scoreMetier, moyenne, tauxReussite, prepJours, nbGaps, recommandation);
            String llm = examenLlmClient.chatCompletionCoach(prompt);
            if (llm == null || llm.isBlank()) {
                return fallback;
            }
            String out = llm.trim();
            return out.length() > 800 ? out.substring(0, 797) + "..." : out;
        } catch (Exception ignored) {
            return fallback;
        }
    }

    /** Ancien flux Skill / parcours intelligent : microservice retiré. */
    @SuppressWarnings("unused")
    private List<String> gapsDetectesSansMicroserviceSkill(Long freelancerId) {
        return List.of();
    }
}
