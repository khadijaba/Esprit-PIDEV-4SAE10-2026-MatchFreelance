package tn.esprit.evaluation.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Appels au microservice Skill via OpenFeign.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SkillClient {

    private final SkillFeignApi feignApi;

    public List<Map<String, Object>> getSkillsByFreelancer(Long freelancerId) {
        try {
            List<Map<String, Object>> body = feignApi.getSkillsByFreelancer(freelancerId);
            return body != null ? body : Collections.emptyList();
        } catch (Exception e) {
            log.warn("Skill MS indisponible pour freelancer {}: {}", freelancerId, e.getMessage());
            return Collections.emptyList();
        }
    }

    /** Crée une compétence (POST /skills). Retourne l'id créé ou null. */
    public Long createSkill(Map<String, Object> skillPayload) {
        try {
            Map<String, Object> body = feignApi.createSkill(skillPayload);
            if (body == null || body.get("id") == null) {
                return null;
            }
            Object id = body.get("id");
            if (id instanceof Number n) {
                return n.longValue();
            }
            return Long.parseLong(id.toString());
        } catch (Exception e) {
            log.warn("Création skill échouée: {}", e.getMessage());
            return null;
        }
    }
}
