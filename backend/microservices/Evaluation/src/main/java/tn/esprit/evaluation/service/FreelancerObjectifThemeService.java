package tn.esprit.evaluation.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.evaluation.dto.ObjectifThemeDto;
import tn.esprit.evaluation.dto.ObjectifThemeRequest;
import tn.esprit.evaluation.entity.FreelancerObjectifTheme;
import tn.esprit.evaluation.entity.FreelancerThemeScoreLog;
import tn.esprit.evaluation.exception.ResourceNotFoundException;
import tn.esprit.evaluation.repository.FreelancerObjectifThemeRepository;
import tn.esprit.evaluation.repository.FreelancerThemeScoreLogRepository;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FreelancerObjectifThemeService {

    private final FreelancerObjectifThemeRepository objectifRepository;
    private final FreelancerThemeScoreLogRepository scoreLogRepository;

    @Transactional(readOnly = true)
    public List<ObjectifThemeDto> listerPourFreelancer(Long freelancerId) {
        return objectifRepository.findByFreelancerIdAndActifTrueOrderByCreatedAtDesc(freelancerId).stream()
                .map(o -> toDto(freelancerId, o))
                .collect(Collectors.toList());
    }

    private ObjectifThemeDto toDto(Long freelancerId, FreelancerObjectifTheme o) {
        Integer dernier = scoreLogRepository
                .findTopByFreelancerIdAndExamenIdAndThemeOrderByCreatedAtDesc(
                        freelancerId, o.getExamenId(), o.getTheme())
                .map(FreelancerThemeScoreLog::getScorePercent)
                .orElse(null);
        boolean atteint = dernier != null && o.getObjectifScore() != null && dernier >= o.getObjectifScore();
        return ObjectifThemeDto.builder()
                .id(o.getId())
                .examenId(o.getExamenId())
                .theme(o.getTheme())
                .objectifScore(o.getObjectifScore())
                .actif(o.isActif())
                .dernierScoreTheme(dernier)
                .objectifAtteint(atteint)
                .build();
    }

    @Transactional
    public ObjectifThemeDto creer(Long freelancerId, ObjectifThemeRequest req) {
        if (req == null || req.getExamenId() == null || req.getTheme() == null || req.getTheme().isBlank()) {
            throw new RuntimeException("examenId et theme sont requis.");
        }
        int cible = req.getObjectifScore() != null ? req.getObjectifScore() : 80;
        if (cible < 0 || cible > 100) {
            throw new RuntimeException("objectifScore doit être entre 0 et 100.");
        }
        FreelancerObjectifTheme o = FreelancerObjectifTheme.builder()
                .freelancerId(freelancerId)
                .examenId(req.getExamenId())
                .theme(req.getTheme().trim())
                .objectifScore(cible)
                .actif(true)
                .build();
        o = objectifRepository.save(o);
        return toDto(freelancerId, o);
    }

    @Transactional
    public void supprimer(Long freelancerId, Long objectifId) {
        FreelancerObjectifTheme o = objectifRepository.findById(objectifId)
                .orElseThrow(() -> new ResourceNotFoundException("Objectif", objectifId));
        if (!o.getFreelancerId().equals(freelancerId)) {
            throw new RuntimeException("Objectif non autorisé.");
        }
        objectifRepository.delete(o);
    }
}
