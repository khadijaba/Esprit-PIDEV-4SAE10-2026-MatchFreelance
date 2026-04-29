<<<<<<< HEAD
=======
<<<<<<< HEAD
package esprit.project.client;

import esprit.project.dto.UserDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Client OpenFeign pour appeler le microservice USER (via Eureka).
 * name = nom du service dans Eureka, path = base path des endpoints.
 */
@FeignClient(name = "USER", path = "/users")
public interface UserClient {

    @GetMapping("/{id}")
    UserDto getUserById(@PathVariable("id") Long id);
}
=======
>>>>>>> b7e93fa9abcd913d3ba37913b8481d5dd480ed43
package esprit.project.client;

import esprit.project.dto.UserDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Client OpenFeign pour appeler le microservice USER (via Eureka).
 * name = nom du service dans Eureka, path = base path des endpoints.
 */
@FeignClient(name = "USER", contextId = "projectUserApi", path = "/users")
public interface UserClient {

    @GetMapping("/{id}")
    UserDto getUserById(@PathVariable("id") Long id);
}
<<<<<<< HEAD
=======
>>>>>>> 8d5250d (Ajout du projet MatchFreelance)
>>>>>>> b7e93fa9abcd913d3ba37913b8481d5dd480ed43
