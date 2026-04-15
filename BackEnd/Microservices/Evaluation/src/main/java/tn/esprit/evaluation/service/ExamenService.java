package tn.esprit.evaluation.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.evaluation.domain.NiveauDifficulteQuestion;
import tn.esprit.evaluation.domain.TypeParcours;
import tn.esprit.evaluation.ExamenMetierConstants;
import tn.esprit.evaluation.client.FormationClient;
import tn.esprit.evaluation.dto.AjouterQuestionsModeleRequest;
import tn.esprit.evaluation.dto.ExamenDto;
import tn.esprit.evaluation.dto.QuestionDto;
import tn.esprit.evaluation.entity.Examen;
import tn.esprit.evaluation.entity.ParcoursInclusion;
import tn.esprit.evaluation.entity.Question;
import tn.esprit.evaluation.exception.ResourceNotFoundException;
import tn.esprit.evaluation.repository.ExamenRepository;
import tn.esprit.evaluation.repository.QuestionRepository;
import tn.esprit.evaluation.util.QcmReponseNormalizer;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExamenService {

    private final ExamenRepository examenRepository;
    private final QuestionRepository questionRepository;
    private final FormationClient formationClient;
    private final FreelancerQuestionTraceService freelancerQuestionTraceService;

    @Transactional(readOnly = true)
    public List<ExamenDto> findAll() {
        return examenRepository.findAll().stream()
                .map(ExamenDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ExamenDto> findByFormationId(Long formationId) {
        return examenRepository.findByFormationIdWithQuestions(formationId).stream()
                .map(ExamenDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ExamenDto findById(Long id) {
        Examen e = examenRepository.findByIdWithQuestions(id)
                .orElseThrow(() -> new ResourceNotFoundException("Examen", id));
        return ExamenDto.fromEntity(e);
    }

    /** Retourne l'examen avec les questions pour le passage (sous-ensemble selon le parcours). */
    @Transactional(readOnly = true)
    public ExamenDto getExamenPourPassage(Long id, TypeParcours typeParcours) {
        Examen e = examenRepository.findByIdWithQuestions(id)
                .orElseThrow(() -> new ResourceNotFoundException("Examen", id));
        TypeParcours tp = typeParcours != null ? typeParcours : TypeParcours.STANDARD;
        List<Question> filtered = QuestionParcoursFilter.filterForParcours(e.getQuestions(), tp);
        Set<Long> keepIds = filtered.stream().map(Question::getId).filter(Objects::nonNull).collect(Collectors.toSet());
        ExamenDto dto = ExamenDto.fromEntity(e);
        if (dto.getQuestions() != null && !keepIds.isEmpty()) {
            dto.setQuestions(dto.getQuestions().stream()
                    .filter(q -> q.getId() == null || keepIds.contains(q.getId()))
                    .collect(Collectors.toList()));
        }
        if (dto.getQuestions() != null) {
            dto.getQuestions().forEach(q -> q.setBonneReponse(null));
        }
        return dto;
    }

    /**
     * Sous-ensemble des questions déjà ratées au moins une fois par ce freelancer (hors certifiant : historique d’entraînement / tentatives).
     */
    @Transactional(readOnly = true)
    public ExamenDto getExamenPourRevision(Long id, TypeParcours typeParcours, Long freelancerId) {
        if (freelancerId == null) {
            throw new RuntimeException("freelancerId est requis pour la révision ciblée.");
        }
        ExamenDto dto = getExamenPourPassage(id, typeParcours);
        List<Long> ratées = freelancerQuestionTraceService.listerQuestionIdsRatéesHistoriquement(freelancerId, id);
        if (dto.getQuestions() == null) {
            return dto;
        }
        if (ratées.isEmpty()) {
            dto.setQuestions(new ArrayList<>());
            return dto;
        }
        Set<Long> keep = new HashSet<>(ratées);
        List<QuestionDto> filtered = dto.getQuestions().stream()
                .filter(q -> q.getId() != null && keep.contains(q.getId()))
                .sorted(Comparator.comparing(QuestionDto::getOrdre, Comparator.nullsLast(Comparator.naturalOrder())))
                .collect(Collectors.toList());
        dto.setQuestions(filtered);
        return dto;
    }

    @Transactional
    public ExamenDto create(ExamenDto dto) {
        if (dto.getQuestions() == null || dto.getQuestions().size() < ExamenMetierConstants.MIN_QUESTIONS_PAR_EXAMEN) {
            throw new RuntimeException("Un examen doit contenir au moins "
                    + ExamenMetierConstants.MIN_QUESTIONS_PAR_EXAMEN
                    + " questions (actuellement : "
                    + (dto.getQuestions() == null ? 0 : dto.getQuestions().size())
                    + ").");
        }
        Examen e = Examen.builder()
                .formationId(dto.getFormationId())
                .titre(dto.getTitre())
                .description(dto.getDescription())
                .seuilReussi(dto.getSeuilReussi() != null ? dto.getSeuilReussi() : 60)
                .build();
        e = examenRepository.save(e);
        if (dto.getQuestions() != null && !dto.getQuestions().isEmpty()) {
            int ordre = 0;
            for (QuestionDto qd : dto.getQuestions()) {
                String enonce = (qd.getEnonce() != null && !qd.getEnonce().isBlank()) ? qd.getEnonce().trim() : ("Question " + (ordre + 1));
                String br = QcmReponseNormalizer.extraireLettreBonneReponse(qd.getBonneReponse());
                if (br.isEmpty()) {
                    br = "A";
                }
                ParcoursInclusion inclusion = parseParcoursInclusion(qd.getParcoursInclusion());
                NiveauDifficulteQuestion niveau = parseNiveauDifficulte(qd.getNiveauDifficulte());
                String theme = qd.getTheme() != null ? qd.getTheme().trim() : null;
                if (theme != null && theme.isEmpty()) theme = null;
                String skill = qd.getSkill() != null ? qd.getSkill().trim() : null;
                if (skill != null && skill.isEmpty()) skill = null;
                String explication = qd.getExplication() != null ? qd.getExplication().trim() : null;
                if (explication != null && explication.isEmpty()) explication = null;
                Question q = Question.builder()
                        .examen(e)
                        .ordre(ordre++)
                        .enonce(enonce)
                        .optionA(qd.getOptionA())
                        .optionB(qd.getOptionB())
                        .optionC(qd.getOptionC())
                        .optionD(qd.getOptionD())
                        .bonneReponse(br)
                        .parcoursInclusion(inclusion)
                        .niveauDifficulte(niveau)
                        .theme(theme)
                        .skill(skill)
                        .explication(explication)
                        .build();
                questionRepository.save(q);
            }
        }
        Examen finalE = e;
        return ExamenDto.fromEntity(examenRepository.findByIdWithQuestions(e.getId()).orElseThrow(() -> new ResourceNotFoundException("Examen", finalE.getId())));
    }

    /**
     * Ajoute des questions modèle (énoncé / options à compléter) pour corriger un examen incomplet par parcours.
     */
    @Transactional
    public ExamenDto appendModeleQuestions(Long examenId, AjouterQuestionsModeleRequest req) {
        int count = req != null && req.getNombre() != null ? req.getNombre() : 1;
        if (count < 1 || count > 50) {
            throw new RuntimeException("Le nombre de questions à ajouter doit être entre 1 et 50.");
        }
        Examen e = examenRepository.findById(examenId)
                .orElseThrow(() -> new ResourceNotFoundException("Examen", examenId));
        List<Question> existing = questionRepository.findByExamenIdOrderByOrdreAsc(examenId);
        int baseOrdre = existing.stream()
                .map(Question::getOrdre)
                .filter(Objects::nonNull)
                .mapToInt(Integer::intValue)
                .max()
                .orElse(-1) + 1;

        ParcoursInclusion inclusion = parseParcoursInclusion(req != null ? req.getParcoursInclusion() : null);

        List<Map<String, Object>> modules = formationClient.getModulesByFormation(e.getFormationId());
        if (modules != null && !modules.isEmpty()) {
            modules = new ArrayList<>(modules);
            modules.sort(Comparator.comparingInt(m -> ExamenQuestionRedactionUtil.parseOrdreModule(m.get("ordre"))));
        }
        String formationTitre = ExamenQuestionRedactionUtil.str(
                formationClient.getFormationById(e.getFormationId()).get("titre"));

        for (int i = 0; i < count; i++) {
            int ordre = baseOrdre + i;
            int variante = existing.size() + i;
            NiveauDifficulteQuestion niveauI = niveauPourOrdreAppend(ordre);

            String[] contenu;
            String theme;
            Map<String, Object> mod = ExamenQuestionRedactionUtil.modulePourIndex(modules, variante);
            if (mod != null) {
                String modTitre = ExamenQuestionRedactionUtil.str(mod.get("titre"));
                if (modTitre.isBlank()) {
                    modTitre = "Module " + (Math.floorMod(variante, modules.size()) + 1);
                }
                String desc = ExamenQuestionRedactionUtil.str(mod.get("description"));
                contenu = ExamenQuestionRedactionUtil.redigerQcmDepuisModule(modTitre, desc, variante);
                theme = ExamenQuestionRedactionUtil.themeDepuisModule(modTitre);
            } else {
                contenu = ExamenQuestionRedactionUtil.redigerQcmDepuisFormationSeule(formationTitre, variante);
                theme = ExamenQuestionRedactionUtil.themeDepuisModule(
                        formationTitre.isBlank() ? "Formation " + e.getFormationId() : formationTitre);
            }

            Question q = Question.builder()
                    .examen(e)
                    .ordre(ordre)
                    .enonce(contenu[0])
                    .optionA(contenu[1])
                    .optionB(contenu[2])
                    .optionC(contenu[3])
                    .optionD(contenu[4])
                    .bonneReponse("A")
                    .parcoursInclusion(inclusion)
                    .niveauDifficulte(req != null && req.getNiveauDifficulte() != null && !req.getNiveauDifficulte().isBlank()
                            ? parseNiveauDifficulte(req.getNiveauDifficulte())
                            : niveauI)
                    .theme(theme)
                    .skill(theme)
                    .build();
            questionRepository.save(q);
        }
        return ExamenDto.fromEntity(examenRepository.findByIdWithQuestions(examenId)
                .orElseThrow(() -> new ResourceNotFoundException("Examen", examenId)));
    }

    @Transactional
    public ExamenDto update(Long id, ExamenDto dto) {
        Examen e = examenRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Examen", id));
        e.setTitre(dto.getTitre());
        e.setDescription(dto.getDescription());
        e.setSeuilReussi(dto.getSeuilReussi() != null ? dto.getSeuilReussi() : e.getSeuilReussi());
        e.setFormationId(dto.getFormationId() != null ? dto.getFormationId() : e.getFormationId());
        return ExamenDto.fromEntity(examenRepository.save(e));
    }

    @Transactional
    public void deleteById(Long id) {
        if (!examenRepository.existsById(id))
            throw new ResourceNotFoundException("Examen", id);
        examenRepository.deleteById(id);
    }

    private static NiveauDifficulteQuestion niveauPourOrdreAppend(int ordre) {
        return switch (Math.floorMod(ordre, 3)) {
            case 0 -> NiveauDifficulteQuestion.FACILE;
            case 1 -> NiveauDifficulteQuestion.MOYEN;
            default -> NiveauDifficulteQuestion.DIFFICILE;
        };
    }

    private static ParcoursInclusion parseParcoursInclusion(String raw) {
        if (raw == null || raw.isBlank()) {
            return ParcoursInclusion.COMMUN;
        }
        try {
            return ParcoursInclusion.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return ParcoursInclusion.COMMUN;
        }
    }

    private static NiveauDifficulteQuestion parseNiveauDifficulte(String raw) {
        if (raw == null || raw.isBlank()) {
            return NiveauDifficulteQuestion.MOYEN;
        }
        try {
            return NiveauDifficulteQuestion.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return NiveauDifficulteQuestion.MOYEN;
        }
    }
}
