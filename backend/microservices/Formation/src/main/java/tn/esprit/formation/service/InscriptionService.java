package tn.esprit.formation.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.formation.dto.InscriptionDto;
import tn.esprit.formation.entity.Formation;
import tn.esprit.formation.entity.Inscription;
import tn.esprit.formation.repository.FormationRepository;
import tn.esprit.formation.repository.InscriptionRepository;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InscriptionService {

    private final InscriptionRepository inscriptionRepository;
    private final FormationRepository formationRepository;

    @Transactional(readOnly = true)
    public List<InscriptionDto> findByFormation(Long formationId) {
        return inscriptionRepository.findByFormationId(formationId).stream()
                .map(InscriptionDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<InscriptionDto> findByFreelancer(Long freelancerId) {
        return inscriptionRepository.findByFreelancerId(freelancerId).stream()
                .map(InscriptionDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<InscriptionDto> findByStatutEnAttente() {
        return inscriptionRepository.findByStatutOrderByDateInscriptionDesc(Inscription.StatutInscription.EN_ATTENTE).stream()
                .map(InscriptionDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional
    public InscriptionDto inscrire(Long formationId, Long freelancerId) {
        Formation formation = formationRepository.findById(formationId)
                .orElseThrow(() -> new RuntimeException("Formation non trouvée: " + formationId));
        if (inscriptionRepository.existsByFormationIdAndFreelancerId(formationId, freelancerId))
            throw new RuntimeException("Freelancer déjà inscrit à cette formation");
        if (formation.getCapaciteMax() != null &&
                inscriptionRepository.findByFormationId(formationId).size() >= formation.getCapaciteMax())
            throw new RuntimeException("Formation complète");
        if (formation.getStatut() != Formation.StatutFormation.OUVERTE)
            throw new RuntimeException("Formation non ouverte aux inscriptions");

        Inscription ins = Inscription.builder()
                .formation(formation)
                .freelancerId(freelancerId)
                .statut(Inscription.StatutInscription.EN_ATTENTE)
                .build();
        return InscriptionDto.fromEntity(inscriptionRepository.save(ins));
    }

    @Transactional
    public InscriptionDto validerInscription(Long inscriptionId) {
        Inscription ins = inscriptionRepository.findById(inscriptionId)
                .orElseThrow(() -> new RuntimeException("Inscription non trouvée: " + inscriptionId));
        ins.setStatut(Inscription.StatutInscription.VALIDEE);
        return InscriptionDto.fromEntity(inscriptionRepository.save(ins));
    }

    @Transactional
    public void annulerInscription(Long inscriptionId) {
        Inscription ins = inscriptionRepository.findById(inscriptionId)
                .orElseThrow(() -> new RuntimeException("Inscription non trouvée: " + inscriptionId));
        ins.setStatut(Inscription.StatutInscription.ANNULEE);
        inscriptionRepository.save(ins);
    }
}
