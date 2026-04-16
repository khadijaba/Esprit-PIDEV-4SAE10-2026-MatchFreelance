package esprit.project.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.Map;


@FeignClient(name = "USER", contextId = "projectUserActuator", path = "/actuator")
public interface UserActuatorClient {

    @GetMapping("/health")
    Map<String, Object> health();
}
