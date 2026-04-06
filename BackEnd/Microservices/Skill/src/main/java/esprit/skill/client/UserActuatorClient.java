package esprit.skill.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.Map;

@FeignClient(name = "USER", contextId = "skillUserActuator", path = "/actuator")
public interface UserActuatorClient {

    @GetMapping("/health")
    Map<String, Object> health();
}
