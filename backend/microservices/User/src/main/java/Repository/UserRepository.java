package Repository;

import Entity.User;
import Entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import java.util.Collection;
import java.util.Optional;
import java.util.List;

public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {
    Optional<User> findByEmail(String email);

    Optional<User> findByEmailIgnoreCase(String email);

    boolean existsByEmail(String email);

    boolean existsByEmailIgnoreCase(String email);
    boolean existsByUserRole(Role userRole);

    List<User> findByUserRole(Role userRole);

    List<User> findByUserRoleIn(Collection<Role> roles);

    List<User> findByEnabled(boolean enabled);
}