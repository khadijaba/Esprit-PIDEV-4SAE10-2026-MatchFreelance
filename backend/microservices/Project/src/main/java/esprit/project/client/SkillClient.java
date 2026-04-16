package esprit.project.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;
import java.util.Map;

@FeignClient(name = "SKILL", path = "/skills")
public interface SkillClient {

    @GetMapping
    List<Map<String, Object>> getAllSkills();
}
