package com.freelancing.candidature.client.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserRemoteDto {
    private Long id;
    private String email;
    private String firstName;
    private String lastName;
    /** Non renvoyé par l'API User ; conservé si un jour le champ existe. */
    private String fullName;

    @JsonAlias("userRole")
    private String role;
}
