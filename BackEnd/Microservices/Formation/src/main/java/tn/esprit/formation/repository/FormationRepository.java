package tn.esprit.formation.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import tn.esprit.formation.entity.Formation;

import java.util.List;

public interface FormationRepository extends JpaRepository<Formation, Long> {

    List<Formation> findByStatut(Formation.StatutFormation statut);

    @Query("SELECT f FROM Formation f WHERE f.statut = 'OUVERTE' ORDER BY f.dateDebut")
    List<Formation> findFormationsOuvertes();
}
