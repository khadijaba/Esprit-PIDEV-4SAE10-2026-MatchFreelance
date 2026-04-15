package tn.esprit.evaluation.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tn.esprit.evaluation.client.FormationClient;
import tn.esprit.evaluation.client.ProjectClient;
import tn.esprit.evaluation.client.SkillClient;
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
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Après obtention du certificat : niveau déduit du score, compétence(s) sur Skill, projets marché triés selon le niveau.
 * Les appels externes sont tolérants aux pannes (ne doit pas faire échouer la soumission d'examen).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CertificatCarriereService {

    private static final Set<String> SKILL_CATEGORIES = Set.of(
            "WEB_DEVELOPMENT", "MOBILE_DEVELOPMENT", "DATA_SCIENCE", "DEVOPS",
            "CYBERSECURITY", "DESIGN", "AI");

    private static final int MAX_THEMES_SKILLS = 3;
    private static final int PROJETS_MARCHE_LIMIT = 5;

    private final CertificatService certificatService;
    private final FormationClient formationClient;
    private final SkillClient skillClient;
    private final ProjectClient projectClient;
    private final QuestionRepository questionRepository;

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
                            + "Sans certificat joint, aucune compétence ni projet n’est synchronisé pour cette réponse.",
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

            int annees = anneesPourNiveau(niveau);
            String nomSkillPrincipal = "Certifié — " + titreFormation + " (examen " + examen.getId() + ")";

            List<CompetenceAttribueeDto> competences = new ArrayList<>();

            SkillClient.AttributionOutcome principal = skillClient.creerCompetenceFreelancer(
                    passage.getFreelancerId(),
                    nomSkillPrincipal,
                    categorie,
                    niveau,
                    annees);
            appendCompetence(competences, nomSkillPrincipal, categorie, niveau, principal);

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
                SkillClient.AttributionOutcome th = skillClient.creerCompetenceFreelancer(
                        passage.getFreelancerId(),
                        nomTheme,
                        categorie,
                        niveau,
                        Math.max(1, annees - 1));
                appendCompetence(competences, nomTheme, categorie, niveau, th);
                themesAjoutes++;
            }

            dto.setCompetencesAttribuees(competences);

            List<Map<String, Object>> brut = projectClient.projetsPourMarcheApresCertificat(categorie, niveau, PROJETS_MARCHE_LIMIT);
            Set<String> jetonsProfil = buildFreelancerSkillTokens(
                    skillClient.getSkillsByFreelancer(passage.getFreelancerId()),
                    categorie);

            List<ProjetMarcheDto> projets = brut.stream()
                    .map(m -> {
                        int alignSkills = scoreAlignementProjet(m, jetonsProfil, categorie);
                        return ProjetMarcheDto.builder()
                                .id(toLong(m.get("id")))
                                .titre(m.get("title") != null ? m.get("title").toString() : null)
                                .budget(toDouble(m.get("budget")))
                                .dureeJours(toInt(m.get("duration")))
                                .statut(m.get("status") != null ? m.get("status").toString() : null)
                                .scoreAlignementSkills(alignSkills)
                                .raison(String.format(
                                        "Adéquation des compétences requises avec votre profil Skill : %d/100. "
                                                + "Domaine formation %s, niveau métier %s (examen %d %%).",
                                        alignSkills, categorie, niveau, score))
                                .build();
                    })
                    .sorted(Comparator.comparingInt(
                            (ProjetMarcheDto projet) ->
                                    projet.getScoreAlignementSkills() != null ? projet.getScoreAlignementSkills() : 0)
                            .reversed())
                    .collect(Collectors.toList());
            dto.setProjetsMarcheRecommandes(projets);
            dto.setMessageCarriere(String.format(
                    "Niveau %s déduit du score (%d %%). %d compétence(s) enregistrée(s) ou signalée(s) sur Skill ; "
                            + "%d projet(s) marché proposé(s), triés par score d’adéquation compétences (meilleur alignement en premier).",
                    niveau, score, competences.size(), projets.size()));
        } catch (Exception e) {
            log.warn("Enrichissement carrière post-certificat partiel ou en échec: {}", e.getMessage(), e);
            dto.setMessageCarriere(String.format(
                    "Niveau « %s » calculé à partir de votre score. "
                            + "La synchronisation avec Formation / Skill / Project a échoué : "
                            + "lancez Eureka, les microservices concernés, puis rechargez cette page.",
                    niveau));
            if (dto.getCompetencesAttribuees() == null || dto.getCompetencesAttribuees().isEmpty()) {
                dto.setCompetencesAttribuees(List.of(CompetenceAttribueeDto.builder()
                        .nom("Synchronisation compétence indisponible")
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

    private static void appendCompetence(
            List<CompetenceAttribueeDto> competences,
            String nom,
            String categorie,
            String niveau,
            SkillClient.AttributionOutcome outcome) {
        CompetenceAttribueeDto.CompetenceAttribueeDtoBuilder cb = CompetenceAttribueeDto.builder()
                .nom(nom)
                .categorie(categorie)
                .niveau(niveau);
        switch (outcome.getKind()) {
            case CREATED -> competences.add(cb.skillId(outcome.getSkillId()).statut("CREE").build());
            case ALREADY_EXISTS -> competences.add(cb.statut("DEJA_PRESENTE").build());
            case FAILED -> competences.add(cb.statut("INDISPONIBLE").build());
        }
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
        String raw = tf != null ? tf.toString().trim().toUpperCase() : "WEB_DEVELOPMENT";
        if (!SKILL_CATEGORIES.contains(raw)) {
            return "WEB_DEVELOPMENT";
        }
        return raw;
    }

    /**
     * Jetons normalisés (catégorie, mots des noms de compétences, niveaux) pour comparer aux {@code requiredSkills} des projets.
     */
    private static Set<String> buildFreelancerSkillTokens(List<Map<String, Object>> skills, String domainCategory) {
        Set<String> t = new HashSet<>();
        if (domainCategory != null && !domainCategory.isBlank()) {
            t.add(domainCategory.toUpperCase(Locale.ROOT));
        }
        for (Map<String, Object> s : skills) {
            Object cat = s.get("category");
            if (cat != null) {
                t.add(cat.toString().toUpperCase(Locale.ROOT));
            }
            Object name = s.get("name");
            if (name != null) {
                for (String w : name.toString().toUpperCase(Locale.ROOT).split("[^A-Z0-9]+")) {
                    if (w.length() > 2) {
                        t.add(w);
                    }
                }
            }
            Object lvl = s.get("level");
            if (lvl != null) {
                t.add(lvl.toString().toUpperCase(Locale.ROOT));
            }
        }
        return t;
    }

    /**
     * 0–100 : part des compétences requises du projet couverte par le profil freelancer (nom / catégorie / domaine).
     */
    private static int scoreAlignementProjet(Map<String, Object> projet, Set<String> profilTokens, String domainCategory) {
        String dom = domainCategory != null ? domainCategory.toUpperCase(Locale.ROOT) : "";
        Object rs = projet.get("requiredSkills");
        if (!(rs instanceof List<?> list) || list.isEmpty()) {
            Object title = projet.get("title");
            if (title != null && !dom.isEmpty() && title.toString().toUpperCase(Locale.ROOT).contains(dom)) {
                return 35;
            }
            return profilTokens.isEmpty() ? 5 : 15;
        }
        int n = list.size();
        int matched = 0;
        for (Object o : list) {
            if (o == null) {
                continue;
            }
            String req = o.toString().toUpperCase(Locale.ROOT);
            if (req.isBlank()) {
                continue;
            }
            boolean ok = false;
            for (String tok : profilTokens) {
                if (tok.length() < 2) {
                    continue;
                }
                if (req.contains(tok) || tok.contains(req)) {
                    ok = true;
                    break;
                }
            }
            if (!ok && !dom.isEmpty() && req.contains(dom)) {
                ok = true;
            }
            if (ok) {
                matched++;
            }
        }
        return (int) Math.round(100.0 * matched / n);
    }

    private static int anneesPourNiveau(String niveau) {
        if ("EXPERT".equals(niveau)) {
            return 4;
        }
        if ("AVANCE".equals(niveau)) {
            return 3;
        }
        if ("INTERMEDIAIRE_SUPERIEUR".equals(niveau)) {
            return 2;
        }
        return 1;
    }

    private static Long toLong(Object o) {
        if (o == null) {
            return null;
        }
        if (o instanceof Number n) {
            return n.longValue();
        }
        try {
            return Long.parseLong(o.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static Double toDouble(Object o) {
        if (o == null) {
            return null;
        }
        if (o instanceof Number n) {
            return n.doubleValue();
        }
        try {
            return Double.parseDouble(o.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static Integer toInt(Object o) {
        if (o == null) {
            return null;
        }
        if (o instanceof Number n) {
            return n.intValue();
        }
        try {
            return Integer.parseInt(o.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
