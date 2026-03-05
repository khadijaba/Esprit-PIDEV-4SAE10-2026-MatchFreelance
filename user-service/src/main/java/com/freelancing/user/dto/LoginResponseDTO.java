package com.freelancing.user.dto;

import com.freelancing.user.enums.UserRole;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponseDTO {

    private String token;
    private Long id;
    private String email;
    private String name;
    private UserRole role;
}
