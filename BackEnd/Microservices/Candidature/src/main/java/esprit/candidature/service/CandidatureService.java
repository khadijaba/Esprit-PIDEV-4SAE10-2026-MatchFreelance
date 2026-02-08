package esprit.candidature.service;


import esprit.candidature.entities.Candidature;
import esprit.candidature.Ripository.CandidatureRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class CandidatureService {

    private final CandidatureRepository candidatureRepository;

    public CandidatureService(CandidatureRepository candidatureRepository) {
        this.candidatureRepository = candidatureRepository;
    }

    // Ajouter une candidature
    public Candidature addCandidature(Long freelancerId, Long projectId) {
        Candidature candidature = new Candidature(freelancerId, projectId, LocalDate.now(), "En attente");
        return candidatureRepository.save(candidature);
    }

    // Récupérer toutes les candidatures
    public List<Candidature> getAll() {
        return candidatureRepository.findAll();
    }

    // Récupérer les candidatures d’un freelance
    public List<Candidature> getByFreelancer(Long freelancerId) {
        return candidatureRepository.findByFreelancerId(freelancerId);
    }

    // Mettre à jour le statut
    public Candidature updateStatut(Long id, String statut) throws Exception {
        Optional<Candidature> c = candidatureRepository.findById(id);
        if (c.isPresent()) {
            c.get().setStatut(statut);
            return candidatureRepository.save(c.get());
        } else {
            throw new Exception("Candidature non trouvée");
        }
    }

    // Supprimer une candidature
    public void deleteCandidature(Long id) {
        candidatureRepository.deleteById(id);
    }
}
