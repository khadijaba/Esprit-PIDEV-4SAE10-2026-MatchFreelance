package tn.esprit.evaluation.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;
import java.util.Map;

@FeignClient(name = "PROJECT")
public interface ProjectFeignApi {

    @GetMapping("/projects/status/{status}")
    List<Map<String, Object>> getProjectsByStatus(@PathVariable("status") String status);
}
