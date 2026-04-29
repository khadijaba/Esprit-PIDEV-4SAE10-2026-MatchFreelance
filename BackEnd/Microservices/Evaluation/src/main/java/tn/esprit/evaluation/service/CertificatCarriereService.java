package tn.esprit.evaluation.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tn.esprit.evaluation.client.FormationClient;
import tn.esprit.evaluation.client.ProjectClient;
import tn.esprit.evaluation.client.SkillClient;
import tn.esprit.evaluation.dto.CertificatDto;
import tn.esprit.evaluation.dto.CompetenceAttribueeDto;
import tn.esprit.evaluation.dto.PassageExamenDto;
import tn.esprit.evaluation.dto.ProjetMarcheDto;
import tn.esprit.evaluation.entity.Examen;
import tn.esprit.evaluation.entity.PassageExamen;
import tn.esprit.evaluation.repository.QuestionRepository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Après certificat : niveau, synthèse, synchronisation optionnelle vers Skill et suggestions Project.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CertificatCarriereService {

    private static final Set<String> SKILL_CATEGORIES = Set.of(
            "WEB_DEVELOPMENT", "MOBILE_DEVELOPMENT", "DATA_SCIENCE", "DEVOPS",
            "CYBERSECURITY", "DESIGN", "AI");

    private static final int MAX_THEMES_SKILLS = 3;

    private final CertificatService certificatService;
    private final FormationClient formationClient;
    private final QuestionRepository questionRepository;
    private final SkillClient skillClient;
    private final ProjectClient projectClient;

    /**
     * Synchronisation de rattrapage : quand un certificat est consulté, on pousse aussi les compétences
     * vers le microservice Skill (idempotent côté Skill par {freelancerId, name}).
     */
    public void synchroniserSkillsDepuisCertificat(CertificatDto cert) {
        if (cert == null || cert.getFreelancerId() == null || cert.getExamenId() == null) {
            return;
        }
        int score = cert.getScore() != null ? cert.getScore() : 0;
        int seuil = cert.getSeuilReussi() != null ? cert.getSeuilReussi() : 60;
        String niveau = calculerNiveau(score, seuil);
        try {
            String categorie = "WEB_DEVELOPMENT";
            String titreFormation = cert.getExamenTitre() != null ? cert.getExamenTitre() : "Formation";
            if (cert.getFormationId() != null) {
                Map<String, Object> formation = formationClient.getFormationById(cert.getFormationId());
                categorie = resolveCategorie(formation);
                if (formation.get("titre") != null) {
                    titreFormation = formation.get("titre").toString();
                }
            }

            String nomSkillPrincipal = "Certifié — " + titreFormation + " (examen " + cert.getExamenId() + ")";
            List<CompetenceAttribueeDto> competences = new ArrayList<>();
            competences.add(CompetenceAttribueeDto.builder()
                    .nom(nomSkillPrincipal)
                    .categorie(categorie)
                    .niveau(niveau)
                    .statut("SYNTHESIS_LOCAL")
                    .build());

            int themesAjoutes = 0;
            for (String themeBrut : questionRepository.findDistinctThemesByExamenId(cert.getExamenId())) {
                if (themesAjoutes >= MAX_THEMES_SKILLS) {
                    break;
                }
                String t = themeBrut != null ? themeBrut.trim() : "";
                if (t.length() < 2) {
                    continue;
                }
                String court = t.length() > 60 ? t.substring(0, 57) + "…" : t;
                String nomTheme = "Thème certifié — " + court;
                if (nomTheme.equalsIgnoreCase(nomSkillPrincipal)) {
                    continue;
                }
                competences.add(CompetenceAttribueeDto.builder()
                        .nom(nomTheme)
                        .categorie(categorie)
                        .niveau(niveau)
                        .statut("SYNTHESIS_LOCAL")
                        .build());
                themesAjoutes++;
            }
            syncSkillsToProfile(cert.getFreelancerId(), competences, categorie);
        } catch (Exception e) {
            log.warn("Synchronisation Skill depuis certificat en échec (certificatId={}): {}",
                    cert.getId(), e.getMessage());
        }
    }

    public void enrichirApresCertificat(PassageExamenDto dto, PassageExamen passage, Examen examen) {
        if (passage.getResultat() != PassageExamen.ResultatExamen.REUSSI) {
            return;
        }
        int score = dto.getScore() != null ? dto.getScore() : 0;
        int seuil = examen.getSeuilReussi() != null ? examen.getSeuilReussi() : 60;
        String niveau = calculerNiveau(score, seuil);
        dto.setNiveauCalcule(niveau);

        if (dto.getCertificat() == null && passage.getId() != null) {
            certificatService.findByPassageExamenIdOptional(passage.getId()).ifPresent(dto::setCertificat);
        }
        if (dto.getCertificat() == null) {
            dto.setMessageCarriere(String.format(
                    "Niveau « %s » calculé à partir de votre score (%d %%). "
                            + "Sans certificat joint, aucune ligne de synthèse n’est ajoutée.",
                    niveau, score));
            dto.setCompetencesAttribuees(Collections.emptyList());
            dto.setProjetsMarcheRecommandes(Collections.emptyList());
            return;
        }

        try {
            Map<String, Object> formation = formationClient.getFormationById(examen.getFormationId());
            String categorie = resolveCategorie(formation);
            String titreFormation = formation.get("titre") != null
                    ? formation.get("titre").toString()
                    : examen.getTitre();

            String nomSkillPrincipal = "Certifié — " + titreFormation + " (examen " + examen.getId() + ")";

            List<CompetenceAttribueeDto> competences = new ArrayList<>();
            competences.add(CompetenceAttribueeDto.builder()
                    .nom(nomSkillPrincipal)
                    .categorie(categorie)
                    .niveau(niveau)
                    .statut("SYNTHESIS_LOCAL")
                    .build());

            int themesAjoutes = 0;
            for (String themeBrut : questionRepository.findDistinctThemesByExamenId(examen.getId())) {
                if (themesAjoutes >= MAX_THEMES_SKILLS) {
                    break;
                }
                String t = themeBrut != null ? themeBrut.trim() : "";
                if (t.length() < 2) {
                    continue;
                }
                String court = t.length() > 60 ? t.substring(0, 57) + "…" : t;
                String nomTheme = "Thème certifié — " + court;
                if (nomTheme.equalsIgnoreCase(nomSkillPrincipal)) {
                    continue;
                }
                competences.add(CompetenceAttribueeDto.builder()
                        .nom(nomTheme)
                        .categorie(categorie)
                        .niveau(niveau)
                        .statut("SYNTHESIS_LOCAL")
                        .build());
                themesAjoutes++;
            }

            dto.setCompetencesAttribuees(competences);
            syncSkillsToProfile(passage.getFreelancerId(), competences, categorie);
            dto.setProjetsMarcheRecommandes(recommendProjectsMarche(categorie, titreFormation));
            dto.setMessageCarriere(String.format(
                    "Niveau %s déduit du score (%d %%). %d ligne(s) de synthèse ; profil Skill synchronisé si le service est disponible.",
                    niveau, score, competences.size()));
        } catch (Exception e) {
            log.warn("Enrichissement carrière post-certificat partiel ou en échec: {}", e.getMessage(), e);
            dto.setMessageCarriere(String.format(
                    "Niveau « %s » calculé à partir de votre score. "
                            + "La synchronisation avec Formation a échoué : vérifiez le microservice Formation.",
                    niveau));
            if (dto.getCompetencesAttribuees() == null || dto.getCompetencesAttribuees().isEmpty()) {
                dto.setCompetencesAttribuees(List.of(CompetenceAttribueeDto.builder()
                        .nom("Synthèse indisponible")
                        .categorie("—")
                        .niveau(niveau)
                        .statut("SERVICE_INDISPONIBLE")
                        .build()));
            }
            if (dto.getProjetsMarcheRecommandes() == null) {
                dto.setProjetsMarcheRecommandes(Collections.emptyList());
            }
        }
    }

    private void syncSkillsToProfile(Long freelancerId, List<CompetenceAttribueeDto> competences, String categorieFormation) {
        if (freelancerId == null) {
            return;
        }
        String skillMsCategory = toSkillMicroserviceCategory(categorieFormation);
        for (CompetenceAttribueeDto c : competences) {
            if (!"SYNTHESIS_LOCAL".equals(c.getStatut())) {
                continue;
            }
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("name", c.getNom());
            body.put("category", skillMsCategory);
            body.put("freelancerId", freelancerId);
            body.put("level", niveauToSkillLevel(c.getNiveau()));
            body.put("yearsOfExperience", 1);
            skillClient.createSkill(body);
        }
    }

    private List<ProjetMarcheDto> recommendProjectsMarche(String categorie, String titreFormation) {
        List<Map<String, Object>> open = projectClient.getProjectsByStatus("OPEN");
        return open.stream()
                .map(p -> buildProjetMarche(p, categorie, titreFormation))
                .sorted(Comparator.comparingInt(ProjetMarcheDto::getScoreAlignementSkills).reversed())
                .limit(5)
                .collect(Collectors.toList());
    }

    private static ProjetMarcheDto buildProjetMarche(Map<String, Object> p, String categorie, String titreFormation) {
        int align = scoreOverlap(p, categorie, titreFormation);
        Long id = toLong(p.get("id"));
        String title = p.get("title") != null ? p.get("title").toString() : "";
        Double budget = p.get("budget") instanceof Number nb ? nb.doubleValue() : null;
        Integer duration = p.get("duration") instanceof Number nb ? nb.intValue() : null;
        String statut = p.get("status") != null ? p.get("status").toString() : "";
        return ProjetMarcheDto.builder()
                .id(id)
                .titre(title)
                .budget(budget)
                .dureeJours(duration)
                .statut(statut)
                .raison("Projet ouvert — proximité avec le domaine certifié")
                .scoreAlignementSkills(align)
                .scoreComposite(align)
                .build();
    }

    private static int scoreOverlap(Map<String, Object> p, String categorie, String titreFormation) {
        Set<String> tokens = new HashSet<>();
        for (String part : (categorie + " " + titreFormation).toLowerCase(Locale.ROOT).split("\\W+")) {
            if (part.length() >= 3) {
                tokens.add(part);
            }
        }
        if (tokens.isEmpty()) {
            return 30;
        }
        String title = p.get("title") != null ? p.get("title").toString().toLowerCase(Locale.ROOT) : "";
        @SuppressWarnings("unchecked")
        List<String> req = (List<String>) p.get("requiredSkills");
        int best = 0;
        for (String t : tokens) {
            if (title.contains(t)) {
                best = Math.max(best, 80);
            }
            if (req != null) {
                for (String r : req) {
                    if (r != null && r.toLowerCase(Locale.ROOT).contains(t)) {
                        best = Math.max(best, 70);
                    }
                }
            }
        }
        return best == 0 ? 35 : best;
    }

    private static Long toLong(Object v) {
        if (v == null) {
            return null;
        }
        if (v instanceof Number n) {
            return n.longValue();
        }
        try {
            return Long.parseLong(v.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** Mappe la catégorie formation vers une catégorie supportée par Skill. */
    private static String toSkillMicroserviceCategory(String formationCategorie) {
        if (formationCategorie == null) {
            return "WEB_DEVELOPMENT";
        }
        String u = formationCategorie.toUpperCase(Locale.ROOT);
        return SKILL_CATEGORIES.contains(u) ? u : "WEB_DEVELOPMENT";
    }

    private static String niveauToSkillLevel(String niveau) {
        if (niveau == null) {
            return "INTERMEDIATE";
        }
        return switch (niveau) {
            case "EXPERT" -> "EXPERT";
            case "AVANCE" -> "ADVANCED";
            case "INTERMEDIAIRE_SUPERIEUR" -> "INTERMEDIATE";
            default -> "INTERMEDIATE";
        };
    }

    private static String calculerNiveau(int score, int seuil) {
        if (score >= 95) {
            return "EXPERT";
        }
        if (score >= 85) {
            return "AVANCE";
        }
        int interSup = Math.max(seuil + 15, 75);
        if (score >= interSup) {
            return "INTERMEDIAIRE_SUPERIEUR";
        }
        return "INTERMEDIAIRE";
    }

    private static String resolveCategorie(Map<String, Object> formation) {
        Object tf = formation.get("typeFormation");
        String raw = tf != null ? tf.toString().trim().toUpperCase(Locale.ROOT) : "WEB_DEVELOPMENT";
        if (!SKILL_CATEGORIES.contains(raw)) {
            return "WEB_DEVELOPMENT";
        }
        return raw;
    }
}
