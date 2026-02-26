package tn.esprit.evaluation.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.evaluation.dto.PassageExamenDto;
import tn.esprit.evaluation.dto.ReponseExamenRequest;
import tn.esprit.evaluation.entity.Examen;
import tn.esprit.evaluation.entity.PassageExamen;
import tn.esprit.evaluation.entity.Question;
import tn.esprit.evaluation.repository.ExamenRepository;
import tn.esprit.evaluation.repository.PassageExamenRepository;
import tn.esprit.evaluation.repository.QuestionRepository;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PassageExamenService {

    private final ExamenRepository examenRepository;
    private final QuestionRepository questionRepository;
    private final PassageExamenRepository passageExamenRepository;
    private final CertificatService certificatService;

    /**
     * Soumet les réponses du freelancer, calcule le score et enregistre le passage.
     * Un freelancer ne peut passer un examen qu'une seule fois (unique constraint).
     */
    @Transactional
    public PassageExamenDto soumettreExamen(Long examenId, ReponseExamenRequest request) {
        Examen examen = examenRepository.findById(examenId)
                .orElseThrow(() -> new RuntimeException("Examen non trouvé: " + examenId));

        if (passageExamenRepository.existsByExamenIdAndFreelancerId(examenId, request.getFreelancerId()))
            throw new RuntimeException("Ce freelancer a déjà passé cet examen.");

        List<Question> questions = questionRepository.findByExamenIdOrderByOrdreAsc(examenId);
        if (questions.isEmpty())
            throw new RuntimeException("Cet examen n'a pas de questions.");

        List<String> reponses = request.getReponses();
        if (reponses == null || reponses.size() != questions.size())
            throw new RuntimeException("Nombre de réponses incorrect. Attendu: " + questions.size());

        int bonnes = 0;
        for (int i = 0; i < questions.size(); i++) {
            String rep = i < reponses.size() ? (reponses.get(i) == null ? "" : reponses.get(i).toUpperCase().trim()) : "";
            if (rep.length() > 0) rep = rep.substring(0, 1);
            if (questions.get(i).getBonneReponse().equalsIgnoreCase(rep))
                bonnes++;
        }
        int score = (int) Math.round(100.0 * bonnes / questions.size());
        PassageExamen.ResultatExamen resultat = score >= examen.getSeuilReussi()
                ? PassageExamen.ResultatExamen.REUSSI
                : PassageExamen.ResultatExamen.ECHOUE;

        PassageExamen passage = PassageExamen.builder()
                .freelancerId(request.getFreelancerId())
                .examen(examen)
                .score(score)
                .resultat(resultat)
                .build();
        passage = passageExamenRepository.save(passage);
        PassageExamenDto dto = PassageExamenDto.fromEntity(passage);
        // Certificat auto-généré si réussi : créé et renvoyé directement dans la réponse
        if (passage.getResultat() == PassageExamen.ResultatExamen.REUSSI) {
            dto.setCertificat(certificatService.creerSiReussi(passage));
        }
        return dto;
    }

    @Transactional(readOnly = true)
    public List<PassageExamenDto> findByFreelancer(Long freelancerId) {
        return passageExamenRepository.findByFreelancerIdOrderByDatePassageDesc(freelancerId).stream()
                .map(PassageExamenDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<PassageExamenDto> findByExamen(Long examenId) {
        return passageExamenRepository.findByExamenIdOrderByDatePassageDesc(examenId).stream()
                .map(PassageExamenDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PassageExamenDto getPassageByFreelancerAndExamen(Long examenId, Long freelancerId) {
        return passageExamenRepository.findByExamenIdAndFreelancerId(examenId, freelancerId)
                .map(PassageExamenDto::fromEntity)
                .orElseThrow(() -> new RuntimeException("Aucun passage trouvé pour cet examen et ce freelancer."));
    }
}
