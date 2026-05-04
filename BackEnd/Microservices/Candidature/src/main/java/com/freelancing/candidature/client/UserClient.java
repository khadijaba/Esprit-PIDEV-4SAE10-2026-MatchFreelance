package com.freelancing.candidature.client;

import com.freelancing.candidature.client.dto.UserRemoteDto;
import feign.FeignException;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
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
        } catch (FeignException.NotFound e) {
            log.warn(
                    "USER: GET /api/users/{} → 404 (utilisateur absent). Si la base USER a été recréée, déconnectez-vous et reconnectez-vous.",
                    id);
            return null;
        } catch (FeignException e) {
            String body = e.contentUTF8() != null ? e.contentUTF8() : "";
            log.warn(
                    "USER: appel Feign échoué pour id={} — HTTP {} — réponse: {}",
                    id,
                    e.status(),
                    body.length() > 500 ? body.substring(0, 500) + "…" : body,
                    e);
            return null;
        } catch (Exception e) {
            log.warn("USER: erreur inattendue pour id={}: {}", id, e.toString(), e);
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
        if (u == null || u.getId() == null) {
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
