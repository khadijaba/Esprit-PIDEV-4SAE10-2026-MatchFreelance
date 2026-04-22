package tn.esprit.formation.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.formation.client.EvaluationClient;
import tn.esprit.formation.dto.InscriptionDto;
import tn.esprit.formation.entity.Formation;
import tn.esprit.formation.entity.Inscription;
import tn.esprit.formation.repository.FormationRepository;
import tn.esprit.formation.repository.InscriptionRepository;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InscriptionService {

    private final InscriptionRepository inscriptionRepository;
    private final FormationRepository formationRepository;
    private final EvaluationClient evaluationClient;

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

        // Condition d'accès : certificat requis (examen Y)
        Long examenRequisId = formation.getExamenRequisId();
        if (examenRequisId != null) {
            List<Map<String, Object>> certificats = evaluationClient.getCertificatsByFreelancer(freelancerId);
            boolean aLeCertificat = certificats.stream().anyMatch(c -> {
                Object id = c.get("examenId");
                if (id == null) return false;
                if (id instanceof Number) return ((Number) id).longValue() == examenRequisId;
                return id.toString().equals(examenRequisId.toString());
            });
            if (!aLeCertificat) {
                String examenTitre = evaluationClient.getExamenTitre(examenRequisId);
                throw new RuntimeException("Accès réservé aux personnes ayant le certificat : " + examenTitre + ".");
            }
        }

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
