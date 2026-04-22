package com.freelancing.contract.client.dto;

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

    @JsonAlias({ "userRole" })
    private String role;
}
