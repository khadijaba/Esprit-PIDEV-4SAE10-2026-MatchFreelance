package tn.esprit.evaluation.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.esprit.evaluation.entity.Certificat;

import java.util.List;
import java.util.Optional;

public interface CertificatRepository extends JpaRepository<Certificat, Long> {

    List<Certificat> findByPassageExamen_FreelancerIdOrderByDateDelivranceDesc(Long freelancerId);

    Optional<Certificat> findByPassageExamenId(Long passageExamenId);

    boolean existsByPassageExamenId(Long passageExamenId);
}
