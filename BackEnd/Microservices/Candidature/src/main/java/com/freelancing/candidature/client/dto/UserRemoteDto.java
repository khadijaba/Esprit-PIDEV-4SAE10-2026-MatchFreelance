package com.freelancing.candidature.client.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserRemoteDto {
    private Long id;
    private String email;
    private String fullName;
    private String firstName;
    private String lastName;

    /** Accepte {@code role} ou {@code userRole} selon la forme du JSON du microservice USER. */
    @JsonAlias({ "userRole" })
    private String role;
}
