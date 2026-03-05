package com.freelancing.user.dto;

import com.freelancing.user.enums.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserRequestDTO {

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    @Size(max = 255)
    private String email;

    @NotBlank(message = "Name is required")
    @Size(min = 2, max = 255)
    private String name;

    @NotNull(message = "Role is required")
    private UserRole role;

    @Size(min = 6, max = 100)
    private String password;
}
