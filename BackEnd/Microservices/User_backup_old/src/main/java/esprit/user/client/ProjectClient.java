package esprit.user.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;
import java.util.Map;

@FeignClient(name = "PROJECT", path = "/projects")
public interface ProjectClient {

    @GetMapping
    List<Map<String, Object>> getAllProjects();
}
