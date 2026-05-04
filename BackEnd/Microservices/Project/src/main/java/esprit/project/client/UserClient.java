package esprit.project.client;

import esprit.project.dto.UserDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Client OpenFeign pour appeler le microservice USER (via Eureka).
 * name = nom du service dans Eureka, path = base path des endpoints.
 */
@FeignClient(name = "USER", contextId = "projectUserApi", path = "/api/users")
public interface UserClient {

    @GetMapping("/{id}")
    UserDto getUserById(@PathVariable("id") Long id);
}
