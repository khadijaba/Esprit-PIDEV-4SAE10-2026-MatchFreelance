package tn.esprit.evaluation.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.evaluation.ExamenMetierConstants;
import tn.esprit.evaluation.domain.TypeParcours;
import tn.esprit.evaluation.client.FormationClient;
import tn.esprit.evaluation.dto.CorrectionItemDto;
import tn.esprit.evaluation.dto.FormationRecoDto;
import tn.esprit.evaluation.dto.PassageExamenDto;
import tn.esprit.evaluation.dto.ReponseExamenRequest;
import tn.esprit.evaluation.dto.SkillScoreDto;
import tn.esprit.evaluation.entity.Examen;
import tn.esprit.evaluation.entity.PassageExamen;
import tn.esprit.evaluation.entity.Question;
import tn.esprit.evaluation.exception.ResourceNotFoundException;
import tn.esprit.evaluation.repository.ExamenRepository;
import tn.esprit.evaluation.repository.PassageExamenRepository;
import tn.esprit.evaluation.repository.QuestionRepository;
import tn.esprit.evaluation.util.QcmReponseNormalizer;
import tn.esprit.evaluation.util.QuestionDifficultyPonderation;

import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PassageExamenService {

    private final ExamenRepository examenRepository;
    private final QuestionRepository questionRepository;
    private final PassageExamenRepository passageExamenRepository;
    private final CertificatService certificatService;
    private final CertificatCarriereService certificatCarriereService;
    private final FormationClient formationClient;
    private final ParcoursApprenantService parcoursApprenantService;
    private final FreelancerQuestionTraceService freelancerQuestionTraceService;
    private final FreelancerThemeScoreLogService freelancerThemeScoreLogService;
    @Value("${app.frontend.base-url:http://localhost:4200}")
    private String frontendBaseUrl;

    /**
     * Soumet les réponses du freelancer, calcule le score et enregistre le passage.
     * Un freelancer ne peut passer un examen qu'une seule fois (unique constraint).
     */
    @Transactional
    public PassageExamenDto soumettreExamen(Long examenId, ReponseExamenRequest request) {
        Examen examen = examenRepository.findById(examenId)
                .orElseThrow(() -> new ResourceNotFoundException("Examen", examenId));

        ReponseExamenRequest.ModePassage mode = request.getMode() != null
                ? request.getMode()
                : ReponseExamenRequest.ModePassage.CERTIFIANT;

        boolean revisionCiblee = Boolean.TRUE.equals(request.getRevisionCiblee());
        if (revisionCiblee && mode != ReponseExamenRequest.ModePassage.ENTRAINEMENT) {
            throw new RuntimeException("La révision ciblée est réservée au mode ENTRAINEMENT.");
        }

        if (mode == ReponseExamenRequest.ModePassage.CERTIFIANT
                && passageExamenRepository.existsByExamenIdAndFreelancerId(examenId, request.getFreelancerId())) {
            throw new RuntimeException("Ce freelancer a déjà passé cet examen.");
        }

        TypeParcours typeParcours = request.getTypeParcours() != null ? request.getTypeParcours() : TypeParcours.STANDARD;
        List<Question> allQuestions = questionRepository.findByExamenIdOrderByOrdreAsc(examenId);
        if (allQuestions.isEmpty()) {
            throw new RuntimeException("Cet examen n'a pas de questions.");
        }
        List<Question> questions = QuestionParcoursFilter.filterForParcours(allQuestions, typeParcours);
        if (questions.isEmpty()) {
            throw new RuntimeException("Aucune question pour ce parcours. Vérifiez les lots COMMUN/STANDARD/RENFORCEMENT.");
        }
        if (revisionCiblee) {
            Set<Long> ratées = new HashSet<>(
                    freelancerQuestionTraceService.listerQuestionIdsRatéesHistoriquement(request.getFreelancerId(), examenId));
            questions = questions.stream()
                    .filter(q -> q.getId() != null && ratées.contains(q.getId()))
                    .sorted(Comparator.comparing(Question::getOrdre, Comparator.nullsLast(Comparator.naturalOrder())))
                    .collect(Collectors.toList());
            if (questions.isEmpty()) {
                throw new RuntimeException(
                        "Aucune question en révision : aucune erreur enregistrée pour cet examen. Passez d'abord un entraînement complet.");
            }
        }
        /* Le minimum de questions ne s'applique qu'au passage certifiant (l'entraînement / révision peut être partiel). */
        boolean skipMinParcours = mode == ReponseExamenRequest.ModePassage.ENTRAINEMENT;
        if (!skipMinParcours && questions.size() < ExamenMetierConstants.MIN_QUESTIONS_PAR_EXAMEN) {
            throw new RuntimeException("Cet examen doit contenir au moins "
                    + ExamenMetierConstants.MIN_QUESTIONS_PAR_EXAMEN
                    + " questions pour ce parcours (actuellement : "
                    + questions.size()
                    + "). Complétez l'examen côté administration, ou utilisez le mode Entraînement pour réviser sans ce minimum.");
        }

        List<String> reponses = request.getReponses();
        if (reponses == null || reponses.size() != questions.size()) {
            throw new RuntimeException("Nombre de réponses incorrect. Attendu: " + questions.size());
        }

        return construireResultatPassage(examen, examenId, request.getFreelancerId(), typeParcours, mode, questions, reponses);
    }

    /**
     * Finalise un passage après une séquence adaptatif (ordre des questions = ordre des réponses).
     */
    @Transactional
    public PassageExamenDto soumettreSequenceAdaptatif(
            Long examenId,
            Long freelancerId,
            TypeParcours typeParcours,
            ReponseExamenRequest.ModePassage mode,
            List<Question> questionsInOrder,
            List<String> reponsesInOrder) {
        Examen examen = examenRepository.findById(examenId)
                .orElseThrow(() -> new ResourceNotFoundException("Examen", examenId));

        if (mode == ReponseExamenRequest.ModePassage.CERTIFIANT
                && passageExamenRepository.existsByExamenIdAndFreelancerId(examenId, freelancerId)) {
            throw new RuntimeException("Ce freelancer a déjà passé cet examen.");
        }
        if (questionsInOrder == null || reponsesInOrder == null || questionsInOrder.size() != reponsesInOrder.size()) {
            throw new RuntimeException("Séquence d'examen adaptatif invalide.");
        }
        if (questionsInOrder.isEmpty()) {
            throw new RuntimeException("Aucune réponse enregistrée pour finaliser l'examen.");
        }
        boolean skipMinAdaptatif = mode == ReponseExamenRequest.ModePassage.ENTRAINEMENT;
        if (!skipMinAdaptatif && questionsInOrder.size() < ExamenMetierConstants.MIN_QUESTIONS_PAR_EXAMEN) {
            throw new RuntimeException("Un passage adaptatif certifiant doit comporter au moins "
                    + ExamenMetierConstants.MIN_QUESTIONS_PAR_EXAMEN
                    + " questions (actuellement : "
                    + questionsInOrder.size()
                    + ").");
        }

        return construireResultatPassage(examen, examenId, freelancerId, typeParcours, mode, questionsInOrder, reponsesInOrder);
    }

    private PassageExamenDto construireResultatPassage(
            Examen examen,
            Long examenId,
            Long freelancerId,
            TypeParcours typeParcours,
            ReponseExamenRequest.ModePassage mode,
            List<Question> questions,
            List<String> reponses) {

        int bonnes = 0;
        int pointsMax = 0;
        int pointsObtenus = 0;
        List<CorrectionItemDto> correction = new java.util.ArrayList<>();
        for (int i = 0; i < questions.size(); i++) {
            Question q = questions.get(i);
            String brute = i < reponses.size() ? reponses.get(i) : null;
            String rep = QcmReponseNormalizer.extraireLettreReponse(brute);
            String bonne = QcmReponseNormalizer.extraireLettreBonneReponse(q.getBonneReponse());
            boolean ok = QcmReponseNormalizer.reponseQcmCorrecte(brute, q.getBonneReponse());
            int poids = QuestionDifficultyPonderation.poids(q.getNiveauDifficulte());
            pointsMax += poids;
            if (ok) {
                bonnes++;
                pointsObtenus += poids;
            }
            String nd = q.getNiveauDifficulte() != null ? q.getNiveauDifficulte().name() : "MOYEN";
            String explication = q.getExplication() != null ? q.getExplication().trim() : null;
            if (explication != null && explication.isEmpty()) {
                explication = null;
            }
            correction.add(CorrectionItemDto.builder()
                    .questionId(q.getId())
                    .ordre(q.getOrdre())
                    .theme(q.getTheme())
                    .skill(effectiveSkillLabel(q))
                    .enonce(q.getEnonce())
                    .reponseChoisie(rep.isEmpty() ? "" : rep)
                    .bonneReponse(bonne.isEmpty() ? "" : bonne)
                    .correct(ok)
                    .niveauDifficulte(nd)
                    .poids(poids)
                    .pointsSurQuestion(ok ? poids : 0)
                    .explication(explication)
                    .build());
        }
        int scoreSansPonderation = QcmReponseNormalizer.pourcentagePlancher(bonnes, questions.size());
        int score = QuestionDifficultyPonderation.pourcentagePlancher(pointsObtenus, pointsMax);
        int erreurs = questions.size() - bonnes;
        PassageExamen.ResultatExamen resultat = score >= examen.getSeuilReussi()
                ? PassageExamen.ResultatExamen.REUSSI
                : PassageExamen.ResultatExamen.ECHOUE;

        PassageExamenDto dto;

        if (mode == ReponseExamenRequest.ModePassage.CERTIFIANT) {
            PassageExamen passage = PassageExamen.builder()
                    .freelancerId(freelancerId)
                    .examen(examen)
                    .score(score)
                    .resultat(resultat)
                    .typeParcours(typeParcours)
                    .build();
            passage = passageExamenRepository.save(passage);
            dto = PassageExamenDto.fromEntity(passage);
            if (passage.getResultat() == PassageExamen.ResultatExamen.REUSSI) {
                dto.setCertificat(certificatService.creerSiReussi(passage));
                certificatCarriereService.enrichirApresCertificat(dto, passage, examen);
            }
        } else {
            dto = PassageExamenDto.builder()
                    .id(null)
                    .freelancerId(freelancerId)
                    .examenId(examen.getId())
                    .examenTitre(examen.getTitre())
                    .score(score)
                    .resultat(resultat)
                    .datePassage(java.time.LocalDateTime.now())
                    .typeParcours(typeParcours)
                    .build();
        }

        dto.setCorrection(correction);
        dto.setScoreParSkill(buildScoreParSkill(questions, reponses));
        dto.setMode(mode);
        dto.setTotalQuestions(questions.size());
        dto.setBonnesReponses(bonnes);
        dto.setScoreSansPonderation(scoreSansPonderation);
        dto.setPointsMax(pointsMax);
        dto.setPointsObtenus(pointsObtenus);
        dto.setTypeParcours(typeParcours);
        dto.setAnalyseErreurs(buildAnalyseErreurs(questions, reponses));
        dto.setMessageFeedback(buildMessageFeedback(score, examen.getSeuilReussi(), erreurs));
        dto.setEvaluationRisque(parcoursApprenantService.evaluerRisque(freelancerId, examenId));
        if (bonnes < questions.size()) {
            dto.setModulesRevisionCibles(
                    parcoursApprenantService.proposerModulesRevision(questions, reponses, examen.getFormationId()));
        }

        List<Map<String, Object>> formations = formationClient.getRecommandationsForFreelancer(freelancerId);
        dto.setFormationsRecommandees(formations.stream().map(f -> FormationRecoDto.builder()
                .id(f.get("id") != null ? Long.valueOf(f.get("id").toString()) : null)
                .titre(f.get("titre") != null ? f.get("titre").toString() : null)
                .typeFormation(f.get("typeFormation") != null ? f.get("typeFormation").toString() : null)
                .niveau(f.get("niveau") != null ? f.get("niveau").toString() : null)
                .statut(f.get("statut") != null ? f.get("statut").toString() : null)
                .dateDebut(f.get("dateDebut") != null ? f.get("dateDebut").toString() : null)
                .dateFin(f.get("dateFin") != null ? f.get("dateFin").toString() : null)
                .lienDirect(buildFormationLink(f.get("id")))
                .build()).collect(Collectors.toList()));

        freelancerQuestionTraceService.enregistrerResultats(freelancerId, questions, reponses);
        if (mode == ReponseExamenRequest.ModePassage.ENTRAINEMENT) {
            freelancerThemeScoreLogService.enregistrerScoresParTheme(freelancerId, examen.getId(), questions, reponses);
        }

        return dto;
    }

    @Transactional(readOnly = true)
    public List<PassageExamenDto> findByFreelancer(Long freelancerId) {
        return passageExamenRepository.findByFreelancerIdWithExamen(freelancerId).stream()
                .map(PassageExamenDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<PassageExamenDto> findByExamen(Long examenId) {
        return passageExamenRepository.findByExamenIdWithExamen(examenId).stream()
                .map(PassageExamenDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PassageExamenDto getPassageByFreelancerAndExamen(Long examenId, Long freelancerId) {
        PassageExamen passage = passageExamenRepository.findByExamenIdAndFreelancerIdWithExamen(examenId, freelancerId)
                .orElseThrow(() -> new ResourceNotFoundException("Passage examen non trouvé pour cet examen et ce freelancer."));
        PassageExamenDto dto = PassageExamenDto.fromEntity(passage);
        if (passage.getResultat() == PassageExamen.ResultatExamen.REUSSI) {
            certificatService.findByPassageExamenIdOptional(passage.getId()).ifPresent(dto::setCertificat);
            certificatCarriereService.enrichirApresCertificat(dto, passage, passage.getExamen());
        }
        return dto;
    }

    private static String effectiveSkillLabel(Question q) {
        if (q.getSkill() != null && !q.getSkill().isBlank()) {
            return q.getSkill().trim();
        }
        if (q.getTheme() != null && !q.getTheme().isBlank()) {
            return q.getTheme().trim();
        }
        return "Autres";
    }

    private List<SkillScoreDto> buildScoreParSkill(List<Question> questions, List<String> reponses) {
        Map<String, int[]> agg = new LinkedHashMap<>();
        for (int i = 0; i < questions.size(); i++) {
            Question q = questions.get(i);
            String skill = effectiveSkillLabel(q);
            String brute = i < reponses.size() ? reponses.get(i) : null;
            boolean ok = QcmReponseNormalizer.reponseQcmCorrecte(brute, q.getBonneReponse());
            int poids = QuestionDifficultyPonderation.poids(q.getNiveauDifficulte());
            int[] row = agg.computeIfAbsent(skill, k -> new int[4]);
            row[1] += poids;
            row[3] += 1;
            if (ok) {
                row[0] += poids;
                row[2] += 1;
            }
        }
        return agg.entrySet().stream()
                .map(e -> {
                    int[] r = e.getValue();
                    int pct = QuestionDifficultyPonderation.pourcentagePlancher(r[0], r[1]);
                    return SkillScoreDto.builder()
                            .skill(e.getKey())
                            .pointsObtenus(r[0])
                            .pointsMax(r[1])
                            .pourcentage(pct)
                            .bonnesReponses(r[2])
                            .totalQuestions(r[3])
                            .build();
                })
                .sorted(Comparator.comparing(SkillScoreDto::getSkill, String.CASE_INSENSITIVE_ORDER))
                .collect(Collectors.toList());
    }

    private String buildFormationLink(Object formationIdRaw) {
        if (formationIdRaw == null) return null;
        String base = frontendBaseUrl == null ? "" : frontendBaseUrl.replaceAll("/+$", "");
        return base + "/formations/" + formationIdRaw;
    }

    private List<String> buildAnalyseErreurs(List<Question> questions, List<String> reponses) {
        List<String> axes = new java.util.ArrayList<>();
        for (int i = 0; i < questions.size(); i++) {
            String brute = i < reponses.size() ? reponses.get(i) : null;
            if (!QcmReponseNormalizer.reponseQcmCorrecte(brute, questions.get(i).getBonneReponse())) {
                axes.add(libelleAxeRevisionPourApprenant(questions.get(i), i + 1));
            }
        }
        if (axes.isEmpty()) {
            axes.add("Aucun axe critique: excellentes réponses.");
        }
        return axes.stream().distinct().limit(5).collect(Collectors.toList());
    }

    /**
     * Libellé lisible pour « Axes à renforcer » : numéro de question (renvoi à la correction détaillée),
     * extrait d’énoncé un peu plus long, et thème en rappel si ce n’est pas déjà dans l’énoncé.
     */
    private String libelleAxeRevisionPourApprenant(Question q, int numeroQuestionDansExamen) {
        String enonce = q.getEnonce() != null ? q.getEnonce().trim() : "";
        String corps;
        if (!isEnoncePlaceholderOuNonFinalise(enonce)) {
            corps = shorten(enonce, 120);
            if (q.getTheme() != null && !q.getTheme().isBlank()) {
                String theme = q.getTheme().trim();
                if (!contientIgnoreCase(enonce, theme)) {
                    corps = corps + " · thème : " + shorten(theme, 48);
                }
            }
        } else if (q.getTheme() != null && !q.getTheme().isBlank()) {
            corps = "thème « " + shorten(q.getTheme().trim(), 72) + " » — notions à consolider";
        } else {
            String module = extraireTitreModuleDepuisEnonce(enonce);
            if (module != null && !module.isBlank()) {
                corps = "module « " + shorten(module, 72) + " »";
            } else {
                corps = "énoncé ou support encore à finaliser côté formation — rapprochez-vous du formateur ou du programme";
            }
        }
        return "Question n°" + numeroQuestionDansExamen + " — À revoir : " + corps;
    }

    private static boolean contientIgnoreCase(String enonce, String fragment) {
        if (enonce == null || fragment == null) {
            return false;
        }
        return enonce.toLowerCase(java.util.Locale.ROOT).contains(fragment.toLowerCase(java.util.Locale.ROOT));
    }

    private static boolean isEnoncePlaceholderOuNonFinalise(String enonce) {
        if (enonce == null || enonce.isBlank()) {
            return true;
        }
        String lower = enonce.toLowerCase();
        if (lower.contains("à compléter par le formateur")) {
            return true;
        }
        if (lower.contains("(option a)") && lower.contains("(option b)")) {
            return true;
        }
        return enonce.matches("(?i)Question\\s+\\d+\\s*[—\\-–]\\s*à compléter.*");
    }

    private static String extraireTitreModuleDepuisEnonce(String enonce) {
        int start = enonce.indexOf('«');
        if (start < 0) {
            start = enonce.indexOf('"');
            if (start >= 0) {
                int end = enonce.indexOf('"', start + 1);
                if (end > start) {
                    return enonce.substring(start + 1, end).trim();
                }
            }
            return null;
        }
        int end = enonce.indexOf('»', start + 1);
        if (end <= start) {
            return null;
        }
        return enonce.substring(start + 1, end).trim();
    }

    private String buildMessageFeedback(int score, Integer seuil, int erreurs) {
        int seuilSafe = seuil != null ? seuil : 60;
        if (score >= seuilSafe) {
            if (score >= 85) return "Excellent resultat. Continue avec des formations avancees pour renforcer ton expertise.";
            return "Bon resultat. Tu es admis, mais quelques revisions ciblees peuvent te faire passer au niveau superieur.";
        }
        if (erreurs >= 3) {
            return "Resultat insuffisant. Recommande: refaire en mode entrainement et suivre les formations suggerees avant une nouvelle tentative.";
        }
        return "Resultat proche du seuil. Revois les points faibles identifies puis repasse en mode certifiant.";
    }

    private String shorten(String value, int max) {
        if (value == null) return "";
        if (value.length() <= max) return value;
        return value.substring(0, Math.max(0, max - 3)) + "...";
    }
}
