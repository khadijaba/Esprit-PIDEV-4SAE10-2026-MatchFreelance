package com.freelancing.candidature.client;

import lombok.Data;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class UserClient {

    private static final String SERVICE_URL = "http://user-service";

    private final RestTemplate restTemplate;

    public UserClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public UserResponse getUserById(Long id) {
        if (id == null) return null;
        try {
            return restTemplate.getForObject(SERVICE_URL + "/api/users/" + id, UserResponse.class);
        } catch (RestClientException e) {
            return null;
        }
    }

    public List<UserResponse> getUsersByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) return Collections.emptyList();
        List<Long> distinct = ids.stream().distinct().filter(id -> id != null).toList();
        if (distinct.isEmpty()) return Collections.emptyList();
        try {
            String idsParam = distinct.stream().map(String::valueOf).collect(Collectors.joining("&ids="));
            UserResponse[] arr = restTemplate.getForObject(SERVICE_URL + "/api/users/ids?ids=" + idsParam, UserResponse[].class);
            return arr != null ? List.of(arr) : Collections.emptyList();
        } catch (RestClientException e) {
            return Collections.emptyList();
        }
    }

    @Data
    public static class UserResponse {
        private Long id;
        private String email;
        private String name;
        private String role;
    }
}
