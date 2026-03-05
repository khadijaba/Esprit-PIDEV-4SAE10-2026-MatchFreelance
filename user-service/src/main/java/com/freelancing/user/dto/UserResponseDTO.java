package com.freelancing.user.dto;

import com.freelancing.user.enums.UserRole;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UserResponseDTO {
    private Long id;
    private String fullName;
    private String email;
    private UserRole role;
    private String addressLine;
    private String city;
    private Double lat;
    private Double lng;
}

