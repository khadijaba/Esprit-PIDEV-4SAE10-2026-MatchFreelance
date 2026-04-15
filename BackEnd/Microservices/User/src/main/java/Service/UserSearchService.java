package Service;

import DTO.UserFilterRequest;
import Entity.Role;
import Entity.User;
import Repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class UserSearchService {

    private final UserRepository userRepository;

    public UserSearchService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public Page<User> searchUsers(UserFilterRequest filter) {
        // Build dynamic specification
        Specification<User> spec = Specification.where(null);

        // Filter by name (first name or last name)
        if (filter.getName() != null && !filter.getName().trim().isEmpty()) {
            spec = spec.and((root, query, criteriaBuilder) ->
                criteriaBuilder.or(
                    criteriaBuilder.like(root.get("firstName"), "%" + filter.getName() + "%"),
                    criteriaBuilder.like(root.get("lastName"), "%" + filter.getName() + "%")
                )
            );
        }

        // Filter by email
        if (filter.getEmail() != null && !filter.getEmail().trim().isEmpty()) {
            spec = spec.and((root, query, criteriaBuilder) ->
                criteriaBuilder.like(root.get("email"), "%" + filter.getEmail() + "%")
            );
        }

        // Filter by role
        if (filter.getRole() != null) {
            spec = spec.and((root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get("userRole"), filter.getRole())
            );
        }

        // Filter by enabled status
        if (filter.getEnabled() != null) {
            spec = spec.and((root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get("enabled"), filter.getEnabled())
            );
        }

        // Create sort (attribut JPA = userRole, pas role)
        String sortBy = filter.getSortBy();
        if (sortBy != null && sortBy.equalsIgnoreCase("role")) {
            sortBy = "userRole";
        }
        Sort sort = Sort.by(
            Sort.Direction.fromString(filter.getSortDir()),
            sortBy
        );

        // Create pageable
        Pageable pageable = PageRequest.of(filter.getPage(), filter.getSize(), sort);

        // Execute search with filters and pagination
        return userRepository.findAll(spec, pageable);
    }

    public List<User> searchByNameOrEmail(String searchTerm) {
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            return new ArrayList<>();
        }

        Specification<User> spec = Specification.where((root, query, criteriaBuilder) ->
            criteriaBuilder.or(
                criteriaBuilder.like(root.get("firstName"), "%" + searchTerm + "%"),
                criteriaBuilder.like(root.get("lastName"), "%" + searchTerm + "%"),
                criteriaBuilder.like(root.get("email"), "%" + searchTerm + "%")
            )
        );

        return userRepository.findAll(spec);
    }

    public List<User> findByRole(Role role) {
        return userRepository.findByUserRole(role);
    }

    public List<User> findActiveUsers() {
        return userRepository.findByEnabled(true);
    }

    public List<User> findInactiveUsers() {
        return userRepository.findByEnabled(false);
    }
}
