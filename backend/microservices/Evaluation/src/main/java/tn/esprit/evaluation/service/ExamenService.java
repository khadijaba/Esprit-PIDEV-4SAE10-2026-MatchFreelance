package tn.esprit.evaluation.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.evaluation.dto.ExamenDto;
import tn.esprit.evaluation.dto.QuestionDto;
import tn.esprit.evaluation.entity.Examen;
import tn.esprit.evaluation.entity.Question;
import tn.esprit.evaluation.repository.ExamenRepository;
import tn.esprit.evaluation.repository.QuestionRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExamenService {

    private final ExamenRepository examenRepository;
    private final QuestionRepository questionRepository;

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
        Examen e = examenRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Examen non trouvé: " + id));
        return ExamenDto.fromEntity(e);
    }

    /** Retourne l'examen avec les questions (sans la bonne réponse pour le passage) */
    @Transactional(readOnly = true)
    public ExamenDto getExamenPourPassage(Long id) {
        Examen e = examenRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Examen non trouvé: " + id));
        ExamenDto dto = ExamenDto.fromEntity(e);
        // On peut masquer bonneReponse côté client si besoin; ici on garde pour correction
        return dto;
    }

    @Transactional
    public ExamenDto create(ExamenDto dto) {
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
                String br = qd.getBonneReponse();
                if (br == null || br.isBlank()) br = "A";
                else br = br.toUpperCase().trim().substring(0, 1);
                if (!"A".equals(br) && !"B".equals(br) && !"C".equals(br) && !"D".equals(br)) br = "A";
                Question q = Question.builder()
                        .examen(e)
                        .ordre(ordre++)
                        .enonce(enonce)
                        .optionA(qd.getOptionA())
                        .optionB(qd.getOptionB())
                        .optionC(qd.getOptionC())
                        .optionD(qd.getOptionD())
                        .bonneReponse(br)
                        .build();
                questionRepository.save(q);
            }
        }
        return ExamenDto.fromEntity(examenRepository.findById(e.getId()).orElseThrow());
    }

    @Transactional
    public ExamenDto update(Long id, ExamenDto dto) {
        Examen e = examenRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Examen non trouvé: " + id));
        e.setTitre(dto.getTitre());
        e.setDescription(dto.getDescription());
        e.setSeuilReussi(dto.getSeuilReussi() != null ? dto.getSeuilReussi() : e.getSeuilReussi());
        e.setFormationId(dto.getFormationId() != null ? dto.getFormationId() : e.getFormationId());
        return ExamenDto.fromEntity(examenRepository.save(e));
    }

    @Transactional
    public void deleteById(Long id) {
        if (!examenRepository.existsById(id))
            throw new RuntimeException("Examen non trouvé: " + id);
        examenRepository.deleteById(id);
    }
}
