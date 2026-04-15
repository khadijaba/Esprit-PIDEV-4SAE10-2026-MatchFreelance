package com.freelancing.candidature.client;

import com.freelancing.candidature.client.dto.UserRemoteDto;
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
            UserRemoteDto u = userRemoteFeign.getUserById(id);
            return toResponse(u);
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
        String name = u.getFullName();
        if (name == null || name.isBlank()) {
            String fn = u.getFirstName() != null ? u.getFirstName() : "";
            String ln = u.getLastName() != null ? u.getLastName() : "";
            name = (fn + " " + ln).trim();
        }
        r.setName(name.isBlank() ? null : name);
        r.setRole(u.getRole());
        return r;
    }

    @Data
    public static class UserResponse {
        private Long id;
        private String email;
        private String name;
        private String role;
    }
}
