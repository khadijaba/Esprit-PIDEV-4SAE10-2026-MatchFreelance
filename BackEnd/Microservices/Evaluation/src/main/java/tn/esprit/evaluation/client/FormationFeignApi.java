package tn.esprit.evaluation.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;
import java.util.Map;

@FeignClient(name = "FORMATION")
public interface FormationFeignApi {

    @GetMapping("/api/formations/recommandations/freelancer/{freelancerId}")
    List<Map<String, Object>> getRecommandationsForFreelancer(@PathVariable("freelancerId") Long freelancerId);

    @GetMapping("/api/modules/formation/{formationId}")
    String getModulesByFormation(@PathVariable("formationId") Long formationId);

    @GetMapping("/api/formations/{formationId}")
    String getFormationById(@PathVariable("formationId") Long formationId);

    @GetMapping("/api/inscriptions/freelancer/{freelancerId}")
    List<Map<String, Object>> getInscriptionsByFreelancer(@PathVariable("freelancerId") Long freelancerId);
}
