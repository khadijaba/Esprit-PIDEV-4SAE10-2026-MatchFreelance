package tn.esprit.formation.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.esprit.formation.entity.Inscription;

import java.util.List;
import java.util.Optional;

public interface InscriptionRepository extends JpaRepository<Inscription, Long> {

    List<Inscription> findByFormationId(Long formationId);

    List<Inscription> findByFreelancerId(Long freelancerId);

    List<Inscription> findByStatutOrderByDateInscriptionDesc(Inscription.StatutInscription statut);

    Optional<Inscription> findByFormationIdAndFreelancerId(Long formationId, Long freelancerId);

    boolean existsByFormationIdAndFreelancerId(Long formationId, Long freelancerId);
}
