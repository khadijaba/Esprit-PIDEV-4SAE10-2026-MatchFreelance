package tn.esprit.formation.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;
import java.util.Map;

@FeignClient(name = "EVALUATION", url = "${app.services.evaluation.url:http://localhost:8083}")
public interface EvaluationFeignApi {

    @GetMapping("/api/certificats/freelancer/{freelancerId}")
    List<Map<String, Object>> getCertificatsByFreelancer(@PathVariable("freelancerId") Long freelancerId);

    @GetMapping("/api/examens/{examenId}")
    Map<String, Object> getExamenById(@PathVariable("examenId") Long examenId);
}
