package tn.esprit.evaluation.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.evaluation.ExamenMetierConstants;
import tn.esprit.evaluation.domain.TypeParcours;
import tn.esprit.evaluation.dto.*;
import tn.esprit.evaluation.entity.ExamenAdaptatifEtape;
import tn.esprit.evaluation.entity.ExamenAdaptatifSession;
import tn.esprit.evaluation.entity.Question;
import tn.esprit.evaluation.exception.ResourceNotFoundException;
import tn.esprit.evaluation.repository.ExamenAdaptatifSessionRepository;
import tn.esprit.evaluation.repository.ExamenRepository;
import tn.esprit.evaluation.repository.PassageExamenRepository;
import tn.esprit.evaluation.repository.QuestionRepository;
import tn.esprit.evaluation.util.QcmReponseNormalizer;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Examen adaptatif : après chaque bonne réponse la difficulté augmente, après une erreur elle diminue.
 */
@Service
@RequiredArgsConstructor
public class ExamenAdaptatifService {

    @Value("${app.examen.adaptatif.nb-questions:7}")
    private int nbQuestionsCible;

    private final ExamenAdaptatifSessionRepository sessionRepository;
    private final ExamenRepository examenRepository;
    private final QuestionRepository questionRepository;
    private final PassageExamenRepository passageExamenRepository;
    private final PassageExamenService passageExamenService;

    @Transactional
    public AdaptatifDemarrageDto demarrer(Long examenId, DemarrerAdaptatifRequest req) {
        if (!examenRepository.existsById(examenId)) {
            throw new ResourceNotFoundException("Examen", examenId);
        }

        ReponseExamenRequest.ModePassage mode = req.getMode() != null
                ? req.getMode()
                : ReponseExamenRequest.ModePassage.CERTIFIANT;

        if (mode == ReponseExamenRequest.ModePassage.CERTIFIANT
                && passageExamenRepository.existsByExamenIdAndFreelancerId(examenId, req.getFreelancerId())) {
            throw new RuntimeException("Ce freelancer a déjà passé cet examen.");
        }

        TypeParcours typeParcours = req.getTypeParcours() != null ? req.getTypeParcours() : TypeParcours.STANDARD;
        List<Question> pool = poolFiltre(examenId, typeParcours);
        int minPool = mode == ReponseExamenRequest.ModePassage.ENTRAINEMENT
                ? 1
                : ExamenMetierConstants.MIN_QUESTIONS_PAR_EXAMEN;
        if (pool.size() < minPool) {
            throw new RuntimeException(mode == ReponseExamenRequest.ModePassage.ENTRAINEMENT
                    ? "Aucune question pour ce parcours (examen adaptatif)."
                    : ("L'examen adaptatif certifiant nécessite au moins "
                    + ExamenMetierConstants.MIN_QUESTIONS_PAR_EXAMEN
                    + " questions pour ce parcours (actuellement : "
                    + pool.size()
                    + "). Ajoutez des questions avec des niveaux FACILE, MOYEN et DIFFICILE."));
        }

        sessionRepository.annulerSessionsEnCours(
                examenId,
                req.getFreelancerId(),
                ExamenAdaptatifSession.StatutSession.EN_COURS,
                ExamenAdaptatifSession.StatutSession.ANNULEE);

        ExamenAdaptatifSession session = ExamenAdaptatifSession.builder()
                .token(UUID.randomUUID().toString())
                .examenId(examenId)
                .freelancerId(req.getFreelancerId())
                .typeParcours(typeParcours)
                .modePassage(mode)
                .difficulteCibleRang(2)
                .statut(ExamenAdaptatifSession.StatutSession.EN_COURS)
                .expiresAt(LocalDateTime.now().plusHours(4))
                .build();

        int minSession = mode == ReponseExamenRequest.ModePassage.ENTRAINEMENT
                ? 1
                : ExamenMetierConstants.MIN_QUESTIONS_PAR_EXAMEN;
        int total = Math.max(minSession, Math.min(nbQuestionsCible, pool.size()));
        Set<Long> vide = new HashSet<>();
        Question first = pickQuestion(pool, vide, 2);
        if (first == null) {
            first = pickRelax(pool, vide, 2);
        }
        if (first == null) {
            throw new RuntimeException("Impossible de choisir une première question pour l'examen adaptatif.");
        }
        session.setQuestionCouranteId(first.getId());
        sessionRepository.save(session);

        return AdaptatifDemarrageDto.builder()
                .token(session.getToken())
                .question(toQuestionSansReponse(first, examenId))
                .numeroQuestion(1)
                .questionsTotal(total)
                .difficulteCible(rankToLabel(2))
                .build();
    }

    @Transactional
    public AdaptatifEtapeReponseDto repondre(Long examenId, String token, AdaptatifRepondreRequest body) {
        ExamenAdaptatifSession session = sessionRepository.findByTokenAndExamenIdWithEtapes(token, examenId)
                .orElseThrow(() -> new ResourceNotFoundException("Session adaptative introuvable ou expirée."));

        if (session.getStatut() != ExamenAdaptatifSession.StatutSession.EN_COURS) {
            throw new RuntimeException("Cette session d'examen n'est plus active.");
        }
        if (session.getExpiresAt().isBefore(LocalDateTime.now())) {
            session.setStatut(ExamenAdaptatifSession.StatutSession.ANNULEE);
            sessionRepository.save(session);
            throw new RuntimeException("Session expirée. Redémarrez un examen adaptatif.");
        }
        if (!Objects.equals(session.getQuestionCouranteId(), body.getQuestionId())) {
            throw new RuntimeException("La question soumise ne correspond pas à la question en cours.");
        }

        List<Question> poolValidation = poolFiltre(examenId, session.getTypeParcours());
        Question q = poolValidation.stream()
                .filter(x -> Objects.equals(x.getId(), body.getQuestionId()))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Question", body.getQuestionId()));

        String rep = QcmReponseNormalizer.extraireLettreReponse(body.getReponse());
        boolean correct = QcmReponseNormalizer.reponseQcmCorrecte(body.getReponse(), q.getBonneReponse());

        int ordre = session.getEtapes().size();
        ExamenAdaptatifEtape etape = ExamenAdaptatifEtape.builder()
                .session(session)
                .questionId(q.getId())
                .reponseChoisie(rep.isEmpty() ? "?" : rep)
                .correct(correct)
                .ordre(ordre)
                .build();
        session.getEtapes().add(etape);

        int nextRank = correct
                ? Math.min(3, session.getDifficulteCibleRang() + 1)
                : Math.max(1, session.getDifficulteCibleRang() - 1);
        session.setDifficulteCibleRang(nextRank);

        List<Question> pool = poolFiltre(examenId, session.getTypeParcours());
        Set<Long> dejaPosees = session.getEtapes().stream()
                .map(ExamenAdaptatifEtape::getQuestionId)
                .collect(Collectors.toSet());
        int minSession = session.getModePassage() == ReponseExamenRequest.ModePassage.ENTRAINEMENT
                ? 1
                : ExamenMetierConstants.MIN_QUESTIONS_PAR_EXAMEN;
        int totalCible = Math.max(minSession, Math.min(nbQuestionsCible, pool.size()));

        if (session.getEtapes().size() >= totalCible) {
            return finaliserSession(session, examenId, correct, nextRank);
        }

        Question next = pickQuestion(pool, dejaPosees, nextRank);
        if (next == null) {
            next = pickRelax(pool, dejaPosees, nextRank);
        }
        if (next == null) {
            return finaliserSession(session, examenId, correct, nextRank);
        }

        session.setQuestionCouranteId(next.getId());
        sessionRepository.save(session);

        return AdaptatifEtapeReponseDto.builder()
                .reponseCorrecte(correct)
                .termine(false)
                .difficulteApresAjustement(rankToLabel(nextRank))
                .numeroQuestion(session.getEtapes().size() + 1)
                .questionsTotal(totalCible)
                .prochaineQuestion(toQuestionSansReponse(next, examenId))
                .build();
    }

    private AdaptatifEtapeReponseDto finaliserSession(
            ExamenAdaptatifSession session,
            Long examenId,
            boolean derniereReponseCorrecte,
            int dernierRang) {

        session.setStatut(ExamenAdaptatifSession.StatutSession.TERMINEE);
        session.setQuestionCouranteId(null);
        sessionRepository.save(session);

        List<ExamenAdaptatifEtape> etapes = new ArrayList<>(session.getEtapes());
        etapes.sort(Comparator.comparingInt(ExamenAdaptatifEtape::getOrdre));
        List<Question> ordered = new ArrayList<>();
        List<String> reponses = new ArrayList<>();
        for (ExamenAdaptatifEtape e : etapes) {
            Question qq = questionRepository.findById(e.getQuestionId())
                    .orElseThrow(() -> new ResourceNotFoundException("Question", e.getQuestionId()));
            ordered.add(qq);
            reponses.add(e.getReponseChoisie());
        }

        PassageExamenDto passage = passageExamenService.soumettreSequenceAdaptatif(
                examenId,
                session.getFreelancerId(),
                session.getTypeParcours(),
                session.getModePassage(),
                ordered,
                reponses);

        return AdaptatifEtapeReponseDto.builder()
                .reponseCorrecte(derniereReponseCorrecte)
                .termine(true)
                .difficulteApresAjustement(rankToLabel(dernierRang))
                .resultat(passage)
                .build();
    }

    private List<Question> poolFiltre(Long examenId, TypeParcours typeParcours) {
        List<Question> all = questionRepository.findByExamenIdOrderByOrdreAsc(examenId);
        return QuestionParcoursFilter.filterForParcours(all, typeParcours);
    }

    private QuestionDto toQuestionSansReponse(Question q, Long examenId) {
        QuestionDto dto = QuestionDto.fromEntity(q, examenId);
        dto.setBonneReponse(null);
        return dto;
    }

    private static int rankOf(Question q) {
        if (q.getNiveauDifficulte() == null) {
            return 2;
        }
        return switch (q.getNiveauDifficulte()) {
            case FACILE -> 1;
            case MOYEN -> 2;
            case DIFFICILE -> 3;
        };
    }

    private static String rankToLabel(int r) {
        return switch (Math.max(1, Math.min(3, r))) {
            case 1 -> "FACILE";
            case 3 -> "DIFFICILE";
            default -> "MOYEN";
        };
    }

    private static Question pickQuestion(List<Question> pool, Set<Long> dejaPosees, int targetRank) {
        List<Question> c = pool.stream()
                .filter(q -> q.getId() != null && !dejaPosees.contains(q.getId()))
                .filter(q -> rankOf(q) == targetRank)
                .collect(Collectors.toList());
        if (c.isEmpty()) {
            return null;
        }
        Collections.shuffle(c);
        return c.get(0);
    }

    private static Question pickRelax(List<Question> pool, Set<Long> dejaPosees, int targetRank) {
        List<Question> avail = pool.stream()
                .filter(q -> q.getId() != null && !dejaPosees.contains(q.getId()))
                .collect(Collectors.toList());
        if (avail.isEmpty()) {
            return null;
        }
        avail.sort(Comparator.comparingInt(q -> Math.abs(rankOf(q) - targetRank)));
        int bestDist = Math.abs(rankOf(avail.get(0)) - targetRank);
        List<Question> tie = avail.stream()
                .filter(q -> Math.abs(rankOf(q) - targetRank) == bestDist)
                .collect(Collectors.toList());
        Collections.shuffle(tie);
        return tie.get(0);
    }
}
