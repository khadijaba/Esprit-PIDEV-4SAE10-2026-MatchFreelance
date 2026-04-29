package tn.esprit.evaluation.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.evaluation.dto.AmenagementTempsDto;
import tn.esprit.evaluation.dto.AmenagementTempsUpsertRequest;
import tn.esprit.evaluation.entity.FreelancerAmenagementTemps;
import tn.esprit.evaluation.repository.FreelancerAmenagementTempsRepository;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class FreelancerAmenagementTempsService {

    private final FreelancerAmenagementTempsRepository repository;

    @Value("${app.examen.chrono.secondes-par-question:10}")
    private int secondesParQuestionBase;

    @Transactional(readOnly = true)
    public AmenagementTempsDto getPourFreelancer(Long freelancerId) {
        double mult = repository.findByFreelancerId(freelancerId)
                .filter(FreelancerAmenagementTemps::isActif)
                .map(FreelancerAmenagementTemps::getMultiplicateurChrono)
                .orElse(1.0);
        mult = clampMult(mult);
        int eff = Math.max(5, (int) Math.round(secondesParQuestionBase * mult));
        return AmenagementTempsDto.builder()
                .secondesParQuestionBase(secondesParQuestionBase)
                .multiplicateurChrono(mult)
                .secondesEffectivesParQuestion(eff)
                .build();
    }

    @Transactional
    public AmenagementTempsDto enregistrer(Long freelancerId, AmenagementTempsUpsertRequest req) {
        double mult = req != null && req.getMultiplicateurChrono() != null
                ? clampMult(req.getMultiplicateurChrono())
                : 1.0;
        FreelancerAmenagementTemps row = repository.findByFreelancerId(freelancerId)
                .orElseGet(() -> FreelancerAmenagementTemps.builder().freelancerId(freelancerId).build());
        row.setMultiplicateurChrono(mult);
        row.setMotif(req != null ? req.getMotif() : null);
        row.setActif(req == null || req.getActif() == null || req.getActif());
        row.setUpdatedAt(LocalDateTime.now());
        repository.save(row);
        return getPourFreelancer(freelancerId);
    }

    private static double clampMult(double m) {
        if (Double.isNaN(m) || m < 1.0) {
            return 1.0;
        }
        if (m > 3.0) {
            return 3.0;
        }
        return m;
    }
}
