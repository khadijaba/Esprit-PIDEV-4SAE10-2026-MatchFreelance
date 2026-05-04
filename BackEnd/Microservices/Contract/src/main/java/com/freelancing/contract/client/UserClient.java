package com.freelancing.contract.client;

import com.freelancing.contract.client.dto.UserRemoteDto;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class UserClient {

    private final UserRemoteFeign userRemoteFeign;

    public UserResponse getUserById(Long id) {
        if (id == null) {
            return null;
        }
        try {
            return toResponse(userRemoteFeign.getUserById(id));
        } catch (Exception e) {
            return null;
        }
    }

    public List<UserResponse> getUsersByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }
        return ids.stream().filter(Objects::nonNull).distinct().map(this::getUserById).filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private static UserResponse toResponse(UserRemoteDto u) {
        if (u == null) {
            return null;
        }
        UserResponse r = new UserResponse();
        r.setId(u.getId());
        r.setEmail(u.getEmail());
        r.setName(resolveDisplayName(u));
        r.setRole(u.getRole());
        return r;
    }

    private static String resolveDisplayName(UserRemoteDto u) {
        if (u.getFullName() != null && !u.getFullName().isBlank()) {
            return u.getFullName().trim();
        }
        String a = u.getFirstName() != null ? u.getFirstName().trim() : "";
        String b = u.getLastName() != null ? u.getLastName().trim() : "";
        String joined = (a + " " + b).trim();
        return joined.isEmpty() ? null : joined;
    }

    @Data
    public static class UserResponse {
        private Long id;
        private String email;
        private String name;
        private String role;
    }
}
