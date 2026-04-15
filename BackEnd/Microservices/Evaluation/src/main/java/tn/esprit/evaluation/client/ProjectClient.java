package tn.esprit.evaluation.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Client vers le microservice Project (Eureka PROJECT) — projets ouverts / compétences requises.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ProjectClient {

    private static final String PROJECT_SERVICE = "http://PROJECT";
    private final RestTemplate restTemplate;

    public List<Map<String, Object>> getProjetsParCompetenceRequise(String skillToken) {
        try {
            List<Map<String, Object>> data = restTemplate.exchange(
                    PROJECT_SERVICE + "/projects/skill/" + skillToken,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<List<Map<String, Object>>>() {}
            ).getBody();
            return data != null ? data : Collections.emptyList();
        } catch (Exception e) {
            log.warn("Project MS skill={} : {}", skillToken, e.getMessage());
            return Collections.emptyList();
        }
    }

    public List<Map<String, Object>> getProjetsOuverts() {
        try {
            List<Map<String, Object>> data = restTemplate.exchange(
                    PROJECT_SERVICE + "/projects/status/OPEN",
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<List<Map<String, Object>>>() {}
            ).getBody();
            return data != null ? data : Collections.emptyList();
        } catch (Exception e) {
            log.warn("Project MS OPEN: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    /** Filtre OPEN et limite. */
    public List<Map<String, Object>> projetsOuvertsMatchingDomaine(String categorieFormation, int limit) {
        List<Map<String, Object>> raw = getProjetsParCompetenceRequise(categorieFormation);
        List<Map<String, Object>> open = raw.stream()
                .filter(p -> "OPEN".equalsIgnoreCase(String.valueOf(p.get("status"))))
                .limit(limit)
                .collect(Collectors.toList());
        if (open.size() >= limit) {
            return open;
        }
        int reste = limit - open.size();
        List<Map<String, Object>> fallback = getProjetsOuverts().stream()
                .filter(p -> "OPEN".equalsIgnoreCase(String.valueOf(p.get("status"))))
                .filter(p -> skillsContainsDomaine(p, categorieFormation))
                .limit(reste)
                .collect(Collectors.toList());
        open.addAll(fallback);
        if (open.size() < limit) {
            getProjetsOuverts().stream()
                    .filter(p -> "OPEN".equalsIgnoreCase(String.valueOf(p.get("status"))))
                    .filter(p -> open.stream().noneMatch(o -> idEq(o, p)))
                    .limit(limit - open.size())
                    .forEach(open::add);
        }
        return open.stream().limit(limit).collect(Collectors.toList());
    }

    /**
     * Projets ouverts après certificat : pool élargi puis tri selon le domaine de formation et le niveau déduit du score.
     */
    public List<Map<String, Object>> projetsPourMarcheApresCertificat(String categorieFormation, String niveauScore, int limit) {
        int maxPool = Math.max(40, limit * 5);
        List<Map<String, Object>> open = new ArrayList<>();
        addOpenUnique(getProjetsParCompetenceRequise(categorieFormation), open, maxPool);
        if (open.size() < maxPool) {
            getProjetsOuverts().stream()
                    .filter(p -> "OPEN".equalsIgnoreCase(String.valueOf(p.get("status"))))
                    .filter(p -> skillsContainsDomaine(p, categorieFormation))
                    .filter(p -> open.stream().noneMatch(o -> idEq(o, p)))
                    .limit(maxPool - open.size())
                    .forEach(open::add);
        }
        if (open.size() < maxPool) {
            getProjetsOuverts().stream()
                    .filter(p -> "OPEN".equalsIgnoreCase(String.valueOf(p.get("status"))))
                    .filter(p -> open.stream().noneMatch(o -> idEq(o, p)))
                    .limit(maxPool - open.size())
                    .forEach(open::add);
        }
        String niveau = niveauScore != null ? niveauScore : "INTERMEDIAIRE";
        open.sort((a, b) -> Double.compare(matchScoreMarche(b, categorieFormation, niveau),
                matchScoreMarche(a, categorieFormation, niveau)));
        return open.stream().limit(limit).collect(Collectors.toList());
    }

    private static void addOpenUnique(List<Map<String, Object>> raw, List<Map<String, Object>> open, int maxTotal) {
        for (Map<String, Object> p : raw) {
            if (open.size() >= maxTotal) {
                break;
            }
            if (!"OPEN".equalsIgnoreCase(String.valueOf(p.get("status")))) {
                continue;
            }
            if (open.stream().noneMatch(o -> idEq(o, p))) {
                open.add(p);
            }
        }
    }

    private static double matchScoreMarche(Map<String, Object> p, String categorie, String niveau) {
        double s = 0;
        if (skillsContainsDomaine(p, categorie)) {
            s += 40;
        }
        Double budget = toDoubleObj(p.get("budget"));
        Integer duration = toIntObj(p.get("duration"));
        if (budget != null) {
            s += switch (niveau) {
                case "EXPERT", "AVANCE" -> Math.min(35, budget / 2500.0);
                case "INTERMEDIAIRE_SUPERIEUR" -> Math.min(30, budget / 3000.0);
                default -> Math.min(25, 8000.0 / (budget + 800));
            };
        }
        if (duration != null && duration > 0) {
            s += switch (niveau) {
                case "EXPERT", "AVANCE" -> Math.min(15, duration / 10.0);
                default -> Math.min(15, 72.0 / (duration + 6));
            };
        }
        return s;
    }

    private static Double toDoubleObj(Object o) {
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

    private static Integer toIntObj(Object o) {
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

    private static boolean idEq(Map<String, Object> a, Map<String, Object> b) {
        Object ida = a.get("id");
        Object idb = b.get("id");
        return ida != null && ida.equals(idb);
    }

    @SuppressWarnings("unchecked")
    private static boolean skillsContainsDomaine(Map<String, Object> project, String categorie) {
        Object rs = project.get("requiredSkills");
        if (!(rs instanceof List)) {
            return false;
        }
        for (Object o : (List<?>) rs) {
            if (o != null && o.toString().toUpperCase().contains(categorie.toUpperCase())) {
                return true;
            }
        }
        return false;
    }
}
