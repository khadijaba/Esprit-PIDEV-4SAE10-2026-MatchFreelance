package esprit.skill.Service;

import esprit.skill.dto.FormationDto;
import esprit.skill.dto.ParcoursIntelligentResponse;
import esprit.skill.entities.Skill;
import esprit.skill.entities.SkillCategory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Parcours Intelligent : analyse des compétences actuelles, détection des gaps,
 * proposition de formations ciblées (via microservice Formation).
 */
@Service
public class ParcoursIntelligentService {

    private static final String FORMATION_SERVICE_URL = "http://FORMATION/api/formations";

    /** Toutes les catégories Formation (domaines possibles) pour détecter les gaps. */
    private static final List<String> TOUS_DOMAINES = Arrays.asList(
            "WEB_DEVELOPMENT", "MOBILE_DEVELOPMENT", "DATA_SCIENCE",
            "DEVOPS", "CYBERSECURITY", "DESIGN", "AI"
    );

    private final SkillService skillService;
    private final RestTemplate restTemplate;

    public ParcoursIntelligentService(SkillService skillService, RestTemplate restTemplate) {
        this.skillService = skillService;
        this.restTemplate = restTemplate;
    }

    /**
     * Calcule le parcours intelligent pour un freelancer :
     * - Compétences actuelles
     * - Gaps (domaines sans compétence)
     * - Formations proposées (ouvertes) ciblant les gaps
     */
    public ParcoursIntelligentResponse calculerParcours(Long freelancerId) {
        List<Skill> competences = skillService.getSkillsByFreelancer(freelancerId);
        Set<String> categoriesActuelles = competences.stream()
                .map(s -> s.getCategory().name())
                .collect(Collectors.toSet());

        List<String> gaps = TOUS_DOMAINES.stream()
                .filter(d -> !categoriesActuelles.contains(d))
                .collect(Collectors.toList());

        List<FormationDto> toutesFormationsOuvertes = fetchFormationsOuvertes();
        List<FormationDto> formationsProposees = toutesFormationsOuvertes.stream()
                .filter(f -> f.getTypeFormation() != null && gaps.contains(f.getTypeFormation()))
                .collect(Collectors.toList());

        return ParcoursIntelligentResponse.builder()
                .freelancerId(freelancerId)
                .competencesActuelles(competences)
                .categoriesActuelles(new ArrayList<>(categoriesActuelles))
                .gapsDetectes(gaps)
                .formationsProposees(formationsProposees)
                .build();
    }

    @SuppressWarnings("unchecked")
    private List<FormationDto> fetchFormationsOuvertes() {
        try {
            String url = FORMATION_SERVICE_URL + "/ouvertes";
            List<LinkedHashMap<String, Object>> body = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<List<LinkedHashMap<String, Object>>>() {}
            ).getBody();
            if (body == null) return Collections.emptyList();
            return body.stream().map(this::mapToFormationDto).collect(Collectors.toList());
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    private FormationDto mapToFormationDto(Map<String, Object> m) {
        FormationDto dto = new FormationDto();
        if (m.get("id") instanceof Number) dto.setId(((Number) m.get("id")).longValue());
        if (m.get("titre") != null) dto.setTitre(m.get("titre").toString());
        if (m.get("typeFormation") != null) dto.setTypeFormation(m.get("typeFormation").toString());
        if (m.get("description") != null) dto.setDescription(m.get("description").toString());
        if (m.get("dureeHeures") instanceof Number) dto.setDureeHeures(((Number) m.get("dureeHeures")).intValue());
        if (m.get("statut") != null) dto.setStatut(m.get("statut").toString());
        return dto;
    }
}
