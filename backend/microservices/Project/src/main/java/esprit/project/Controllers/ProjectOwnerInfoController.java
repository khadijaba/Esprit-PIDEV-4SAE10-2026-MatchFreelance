package esprit.project.Controllers;

import esprit.project.client.UserClient;
import esprit.project.dto.UserDto;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Exemple d'utilisation d'OpenFeign : le microservice Project appelle le microservice User
 * pour récupérer les infos du propriétaire d'un projet.
 */
@RestController
@RequestMapping("/projects")
@CrossOrigin(origins = "*")
public class ProjectOwnerInfoController {

    private final UserClient userClient;

    public ProjectOwnerInfoController(UserClient userClient) {
        this.userClient = userClient;
    }

    /**
     * Récupère les infos du propriétaire (owner) d'un projet via le microservice User (OpenFeign).
     * Exemple : GET /projects/owner/{ownerId}/info
     */
    @GetMapping("/owner/{ownerId}/info")
    public ResponseEntity<UserDto> getOwnerInfo(@PathVariable Long ownerId) {
        try {
            UserDto user = userClient.getUserById(ownerId);
            return ResponseEntity.ok(user);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }
}
