package com.freelancing.user.repository;

import com.freelancing.user.entity.User;
import com.freelancing.user.enums.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    boolean existsByEmail(String email);

    Optional<User> findByEmail(String email);

    List<User> findByIdIn(List<Long> ids);
}
