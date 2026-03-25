package tn.esprit.evaluation.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import tn.esprit.evaluation.entity.Certificat;

import java.util.List;
import java.util.Optional;

public interface CertificatRepository extends JpaRepository<Certificat, Long> {

    List<Certificat> findByPassageExamen_FreelancerIdOrderByDateDelivranceDesc(Long freelancerId);

    /** Charge certificats avec passage et examen en une requête (évite LazyInitializationException). */
    @Query("SELECT DISTINCT c FROM Certificat c JOIN FETCH c.passageExamen p JOIN FETCH p.examen WHERE p.freelancerId = :freelancerId ORDER BY c.dateDelivrance DESC")
    List<Certificat> findByFreelancerIdWithPassageAndExamen(@Param("freelancerId") Long freelancerId);

    Optional<Certificat> findByPassageExamenId(Long passageExamenId);

    @Query("SELECT c FROM Certificat c JOIN FETCH c.passageExamen p JOIN FETCH p.examen WHERE p.id = :passageExamenId")
    Optional<Certificat> findByPassageExamenIdWithPassageAndExamen(@Param("passageExamenId") Long passageExamenId);

    @Query("SELECT c FROM Certificat c JOIN FETCH c.passageExamen p JOIN FETCH p.examen WHERE c.id = :id")
    Optional<Certificat> findByIdWithPassageAndExamen(@Param("id") Long id);

    Optional<Certificat> findByNumeroCertificat(String numeroCertificat);

    @Query("SELECT c FROM Certificat c JOIN FETCH c.passageExamen p JOIN FETCH p.examen WHERE c.numeroCertificat = :numero")
    Optional<Certificat> findByNumeroCertificatWithPassageAndExamen(@Param("numero") String numero);

    boolean existsByPassageExamenId(Long passageExamenId);
}
