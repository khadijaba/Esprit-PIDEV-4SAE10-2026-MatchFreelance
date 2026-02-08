package esprit.candidature.entities;


import jakarta.persistence.*;

import java.time.LocalDate;

@Entity
public class Candidature {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long freelancerId; // Id du freelance
    private Long projectId;    // Id du projet
    private LocalDate dateCandidature;
    private String statut;     // Ex: "En attente", "Acceptée", "Refusée"

    public Candidature() {
    }

    public Candidature(Long freelancerId, Long projectId, LocalDate dateCandidature, String statut) {
        this.freelancerId = freelancerId;
        this.projectId = projectId;
        this.dateCandidature = dateCandidature;
        this.statut = statut;
    }

    // Getters et Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getFreelancerId() {
        return freelancerId;
    }

    public void setFreelancerId(Long freelancerId) {
        this.freelancerId = freelancerId;
    }

    public Long getProjectId() {
        return projectId;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }

    public LocalDate getDateCandidature() {
        return dateCandidature;
    }

    public void setDateCandidature(LocalDate dateCandidature) {
        this.dateCandidature = dateCandidature;
    }

    public String getStatut() {
        return statut;
    }

    public void setStatut(String statut) {
        this.statut = statut;
    }
}
