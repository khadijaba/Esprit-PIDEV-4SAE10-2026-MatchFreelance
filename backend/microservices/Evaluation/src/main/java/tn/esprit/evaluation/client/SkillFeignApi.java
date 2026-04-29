package tn.esprit.evaluation.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;
import java.util.Map;

@FeignClient(name = "SKILL")
public interface SkillFeignApi {

    @GetMapping("/skills/freelancer/{freelancerId}")
    List<Map<String, Object>> getSkillsByFreelancer(@PathVariable("freelancerId") Long freelancerId);

    @PostMapping(value = "/skills", consumes = "application/json")
    Map<String, Object> createSkill(@RequestBody Map<String, Object> skillPayload);
}
